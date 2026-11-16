#Requires -Version 5.1
<#
.SYNOPSIS
  One-off TomTom Flow coverage validation for PDCM pilot expansion.

.DESCRIPTION
  Uses the same zoom fallback order (18→15→12→10), max 750 m nearest-point
  geometry rule, and "HTTP success alone ≠ coverage" policy as
  TomTomTrafficClient + FlowSegmentGeometryValidator.

  Hard caps for this exercise:
    - max 4 Flow requests per candidate
    - max 200 Flow requests total

  Does NOT reset or bypass ProviderBudgetService (standalone tool).
  Never prints or writes the API key.

.NOTES
  Set PDCM_TOMTOM_API_KEY in the environment before running.
#>
[CmdletBinding()]
param(
  [string]$CandidatesCsv = "",
  [string]$ResultsCsv = "",
  [string]$ResultsMd = "",
  [int]$MaxDistanceMeters = 750,
  [int]$MaxRequestsPerCandidate = 4,
  [int]$MaxTotalRequests = 200,
  [int[]]$Zooms = @(18, 15, 12, 10),
  [string]$BaseUrl = "https://api.tomtom.com",
  [string]$FlowPathTemplate = "/traffic/services/4/flowSegmentData/absolute/{zoom}/json"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptRoot = if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) {
  $PSScriptRoot
} else {
  Split-Path -Parent $MyInvocation.MyCommand.Path
}
if ([string]::IsNullOrWhiteSpace($CandidatesCsv)) {
  $CandidatesCsv = Join-Path $scriptRoot "..\docs\data\tomtom-pilot-candidates.csv"
}
if ([string]::IsNullOrWhiteSpace($ResultsCsv)) {
  $ResultsCsv = Join-Path $scriptRoot "..\docs\data\tomtom-pilot-coverage-results.csv"
}
if ([string]::IsNullOrWhiteSpace($ResultsMd)) {
  $ResultsMd = Join-Path $scriptRoot "..\docs\TOMTOM_PILOT_COVERAGE.md"
}

function Write-SafeLog([string]$Message) {
  # Never echo secrets. Key is only read into a local variable and used in URI construction.
  Write-Host $Message
}

function Get-HaversineMeters([double]$Lat1, [double]$Lon1, [double]$Lat2, [double]$Lon2) {
  $earthKm = 6371.0
  $dLat = [Math]::PI / 180.0 * ($Lat2 - $Lat1)
  $dLon = [Math]::PI / 180.0 * ($Lon2 - $Lon1)
  $a = [Math]::Sin($dLat / 2.0) * [Math]::Sin($dLat / 2.0) +
    [Math]::Cos([Math]::PI / 180.0 * $Lat1) * [Math]::Cos([Math]::PI / 180.0 * $Lat2) *
    [Math]::Sin($dLon / 2.0) * [Math]::Sin($dLon / 2.0)
  $c = 2.0 * [Math]::Atan2([Math]::Sqrt($a), [Math]::Sqrt(1.0 - $a))
  return $earthKm * $c * 1000.0
}

function Get-NearestSegmentDistanceMeters([double]$DestLat, [double]$DestLon, $Coordinates) {
  if (-not $Coordinates -or $Coordinates.Count -eq 0) {
    return [double]::PositiveInfinity
  }
  $nearest = [double]::PositiveInfinity
  foreach ($point in $Coordinates) {
    $meters = Get-HaversineMeters $DestLat $DestLon ([double]$point.latitude) ([double]$point.longitude)
    if ($meters -lt $nearest) {
      $nearest = $meters
    }
  }
  return $nearest
}

$apiKey = $env:PDCM_TOMTOM_API_KEY
$candidatesPath = (Resolve-Path $CandidatesCsv).Path
$resultsCsvPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($ResultsCsv)
$resultsMdPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($ResultsMd)

$candidates = Import-Csv -Path $candidatesPath
if (-not $candidates -or $candidates.Count -eq 0) {
  throw "No candidates found in $candidatesPath"
}

$generatedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
$results = New-Object System.Collections.Generic.List[object]
$totalRequests = 0
$usableCount = 0

if ([string]::IsNullOrWhiteSpace($apiKey)) {
  Write-SafeLog "PDCM_TOMTOM_API_KEY is missing. Writing candidate list + MANUAL validation plan (no live TomTom calls)."
  foreach ($row in $candidates) {
    $results.Add([pscustomobject]@{
        priority             = $row.priority
        name                 = $row.name
        slug                 = $row.slug
        latitude             = $row.latitude
        longitude            = $row.longitude
        coverage_outcome     = "NOT_VALIDATED"
        usable_coverage      = "false"
        success_zoom         = ""
        nearest_distance_m   = ""
        frc                  = ""
        provider_confidence  = ""
        validation_requests  = 0
        notes                = "Key missing; manual validation required. selection=$($row.selection_reason)"
      }) | Out-Null
  }
}
else {
  Write-SafeLog "Validating $($candidates.Count) catalogue candidates (max $MaxRequestsPerCandidate zooms each, hard cap $MaxTotalRequests requests)."
  $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

  foreach ($row in $candidates) {
    if ($totalRequests -ge $MaxTotalRequests) {
      $results.Add([pscustomobject]@{
          priority             = $row.priority
          name                 = $row.name
          slug                 = $row.slug
          latitude             = $row.latitude
          longitude            = $row.longitude
          coverage_outcome     = "SKIPPED_TOTAL_CAP"
          usable_coverage      = "false"
          success_zoom         = ""
          nearest_distance_m   = ""
          frc                  = ""
          provider_confidence  = ""
          validation_requests  = 0
          notes                = "Stopped before this candidate: total request cap $MaxTotalRequests reached."
        }) | Out-Null
      continue
    }

    $lat = [double]$row.latitude
    $lon = [double]$row.longitude
    $used = 0
    $outcome = "NO_USABLE_SEGMENT"
    $usable = "false"
    $successZoom = ""
    $nearestOut = ""
    $frcOut = ""
    $confidenceOut = ""
    $notes = ""

    foreach ($zoom in $Zooms) {
      if ($used -ge $MaxRequestsPerCandidate) { break }
      if ($totalRequests -ge $MaxTotalRequests) {
        $notes = "Hit total request cap mid-candidate."
        break
      }

      $path = $FlowPathTemplate.Replace("{zoom}", [string]$zoom)
      $uri = "{0}{1}?point={2},{3}&unit=KMPH&key={4}" -f $BaseUrl.TrimEnd('/'), $path, $lat, $lon, $apiKey
      $used++
      $totalRequests++

      try {
        $response = Invoke-WebRequest -Uri $uri -Method GET -Headers @{ Accept = "application/json" } `
          -TimeoutSec 8 -WebSession $session -UseBasicParsing
        $json = $response.Content | ConvertFrom-Json
        $data = $json.flowSegmentData
        if (-not $data) {
          $notes = "HTTP $($response.StatusCode) but missing flowSegmentData at zoom $zoom"
          continue
        }
        $coords = @()
        if ($data.coordinates -and $data.coordinates.coordinate) {
          $coords = @($data.coordinates.coordinate)
        }
        $nearest = Get-NearestSegmentDistanceMeters $lat $lon $coords
        if ($nearest -gt $MaxDistanceMeters -or [double]::IsInfinity($nearest)) {
          $notes = "Segment too far at zoom $zoom (nearest=${nearest}m > ${MaxDistanceMeters}m)"
          continue
        }

        $outcome = "USABLE"
        $usable = "true"
        $successZoom = [string]$zoom
        $nearestOut = [math]::Round($nearest, 1)
        $frcOut = [string]$data.frc
        if ($null -ne $data.confidence) {
          $confidenceOut = [string]$data.confidence
        }
        $notes = "Nearby approach road within ${MaxDistanceMeters}m at zoom $zoom"
        $usableCount++
        break
      }
      catch {
        $status = $null
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
          $status = [int]$_.Exception.Response.StatusCode
        }
        if ($status -eq 429) {
          $outcome = "RATE_LIMITED"
          $notes = "HTTP 429 — stop further live calls for safety."
          # Mark remaining as skipped after we finish this row.
          break
        }
        $notes = "Request failed at zoom $zoom (status=$status). Message redacted."
      }
    }

    $results.Add([pscustomobject]@{
        priority             = $row.priority
        name                 = $row.name
        slug                 = $row.slug
        latitude             = $row.latitude
        longitude            = $row.longitude
        coverage_outcome     = $outcome
        usable_coverage      = $usable
        success_zoom         = $successZoom
        nearest_distance_m   = $nearestOut
        frc                  = $frcOut
        provider_confidence  = $confidenceOut
        validation_requests  = $used
        notes                = $notes
      }) | Out-Null

    if ($outcome -eq "RATE_LIMITED") {
      Write-SafeLog "Rate limited after $totalRequests requests. Remaining candidates marked skipped."
      $remaining = $false
      foreach ($later in $candidates) {
        if (-not $remaining) {
          if ($later.slug -eq $row.slug) { $remaining = $true }
          continue
        }
        $results.Add([pscustomobject]@{
            priority             = $later.priority
            name                 = $later.name
            slug                 = $later.slug
            latitude             = $later.latitude
            longitude            = $later.longitude
            coverage_outcome     = "SKIPPED_RATE_LIMIT"
            usable_coverage      = "false"
            success_zoom         = ""
            nearest_distance_m   = ""
            frc                  = ""
            provider_confidence  = ""
            validation_requests  = 0
            notes                = "Skipped after HTTP 429."
          }) | Out-Null
      }
      break
    }
  }
}

$results | Export-Csv -Path $resultsCsvPath -NoTypeInformation -Encoding UTF8

$verified = @($results | Where-Object { $_.usable_coverage -eq "true" })
$notValidated = @($results | Where-Object { $_.coverage_outcome -eq "NOT_VALIDATED" })
$failed = @($results | Where-Object { $_.usable_coverage -ne "true" -and $_.coverage_outcome -ne "NOT_VALIDATED" })

$md = New-Object System.Text.StringBuilder
[void]$md.AppendLine("# TomTom pilot coverage validation")
[void]$md.AppendLine("")
[void]$md.AppendLine("Generated (UTC): $generatedAt")
[void]$md.AppendLine("")
[void]$md.AppendLine("## Method")
[void]$md.AppendLine("")
[void]$md.AppendLine("- Candidates: existing catalogue slugs only (see ``docs/data/tomtom-pilot-candidates.csv``).")
[void]$md.AppendLine("- Priority: Delhi NCR first, then famous Indian tourist sites; original five retained.")
[void]$md.AppendLine("- Zoom fallback: 18 → 15 → 12 → 10 (same as ``pdcm.external-traffic.flow-zoom-fallbacks``).")
[void]$md.AppendLine("- Geometry: nearest Flow coordinate must be ≤ **750 m** (``FlowSegmentGeometryValidator`` / ``max-segment-distance-meters``).")
[void]$md.AppendLine("- HTTP success alone is **not** usable coverage.")
[void]$md.AppendLine("- Caps: ≤4 Flow requests/candidate, ≤200 total for this exercise.")
[void]$md.AppendLine("- API key: read from ``PDCM_TOMTOM_API_KEY`` only; never logged or written to these files.")
[void]$md.AppendLine("")
[void]$md.AppendLine("## Summary")
[void]$md.AppendLine("")
[void]$md.AppendLine("| Metric | Value |")
[void]$md.AppendLine("|---|---|")
[void]$md.AppendLine("| Candidates evaluated | $($results.Count) |")
[void]$md.AppendLine("| Usable coverage (verified) | $($verified.Count) |")
[void]$md.AppendLine("| Not validated (key missing / skipped) | $($notValidated.Count) |")
[void]$md.AppendLine("| Failed / unusable | $($failed.Count) |")
[void]$md.AppendLine("| Flow requests used | $totalRequests |")
[void]$md.AppendLine("")

if ($notValidated.Count -gt 0 -and $totalRequests -eq 0) {
  [void]$md.AppendLine("## Manual validation plan (API key missing)")
  [void]$md.AppendLine("")
  [void]$md.AppendLine("Live TomTom calls were **not** made. To finish the ≥50 expansion safely:")
  [void]$md.AppendLine("")
  [void]$md.AppendLine('1. In PowerShell: `setx PDCM_TOMTOM_API_KEY "your_free_tier_key"` then open a **new** shell (or `$env:PDCM_TOMTOM_API_KEY="..."` for the session only).')
  [void]$md.AppendLine("2. Confirm daily/monthly ProviderBudgetService usage in the running API (ADMIN usage endpoint) has enough remaining headroom for ≤200 validation calls — do **not** reset counters.")
  [void]$md.AppendLine("3. From repo root: ``powershell -File scripts/Validate-TomTomPilotCoverage.ps1``")
  [void]$md.AppendLine("4. Add **only** rows with ``usable_coverage=true`` to ``pdcm.external-traffic.pilot-slugs`` in ``application.yml`` **and** ``ExternalTrafficProperties`` defaults. Keep the original five.")
  [void]$md.AppendLine("5. Re-run backend/frontend tests. Do not label unvalidated or failed geometry as supported.")
  [void]$md.AppendLine("")
  [void]$md.AppendLine("Catalogue gaps vs the suggested India list (not present as rows — cannot pilot): Humayun's Tomb, Akshardham, Connaught Place, Rashtrapati Bhavan, Lodhi Garden, Purana Qila, Gurugram Cyber Hub, Sultanpur, Damdama, Neemrana, Mathura, Vrindavan, Chandigarh, Dehradun, Alleppey, Kochi Fort, Golconda, Marina Beach, Somnath.")
  [void]$md.AppendLine("")
}

[void]$md.AppendLine("## Budget mathematics (product, not this one-off)")
[void]$md.AppendLine("")
[void]$md.AppendLine("| Assumption | Math |")
[void]$md.AppendLine("|---|---|")
[void]$md.AppendLine("| Shared cache TTL | 180 minutes → at most ~8 theoretical refreshes/destination/day if every TTL is exhausted |")
[void]$md.AppendLine("| 50 pilots × 8 | 400 refreshes/day |")
[void]$md.AppendLine("| If each refresh is 1 Flow call | 400 × 30 ≈ **12_000**/month |")
[void]$md.AppendLine("| App monthly hard cap | **15_000** → ~3_000 headroom for zoom fallbacks |")
[void]$md.AppendLine("| Worst case 4 zooms every refresh | 1_600/day → **48_000**/month — exceeds free-tier and app cap |")
[void]$md.AppendLine("| Daily hard budget | **500** always wins over desired refresh cadence |")
[void]$md.AppendLine("| TomTom free monthly | ~20_000; PDCM stays under 15_000 Flow and never enables Pay As You Grow |")
[void]$md.AppendLine("")
[void]$md.AppendLine("Schedule refresh and Traffic Incidents stay **off**. No promise of 8 refreshes/day for every pilot. On-demand + shared 3h cache only.")
[void]$md.AppendLine("")
[void]$md.AppendLine("## Results table")
[void]$md.AppendLine("")
[void]$md.AppendLine("| Name | Slug | Coverage outcome | Nearest m | Success zoom | Requests |")
[void]$md.AppendLine("|---|---|---|---|---|---|")
foreach ($r in $results) {
  $near = if ($r.nearest_distance_m -ne "") { $r.nearest_distance_m } else { "—" }
  $zoom = if ($r.success_zoom -ne "") { $r.success_zoom } else { "—" }
  [void]$md.AppendLine("| $($r.name) | ``$($r.slug)`` | $($r.coverage_outcome) | $near | $zoom | $($r.validation_requests) |")
}
[void]$md.AppendLine("")
[void]$md.AppendLine("Machine-readable copy: ``docs/data/tomtom-pilot-coverage-results.csv``.")
[void]$md.AppendLine("")
[void]$md.AppendLine("## Verified slugs for allowlist (usable only)")
[void]$md.AppendLine("")
if ($verified.Count -eq 0) {
  [void]$md.AppendLine("_None verified in this run._ Keep the existing five configured pilots until a live validation pass succeeds.")
}
else {
  foreach ($v in $verified) {
    [void]$md.AppendLine("- ``$($v.slug)``")
  }
}

[System.IO.File]::WriteAllText($resultsMdPath, $md.ToString(), [System.Text.UTF8Encoding]::new($false))

Write-SafeLog "Wrote $resultsCsvPath"
Write-SafeLog "Wrote $resultsMdPath"
Write-SafeLog "Usable verified=$($verified.Count); requests_used=$totalRequests; candidates=$($results.Count)"
