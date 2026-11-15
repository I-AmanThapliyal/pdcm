import { useEffect, useRef, type MutableRefObject } from 'react'
import { MapContainer, Marker, Popup, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'
import type { Destination } from '../types'
import { INDIA_MAP_CENTER, INDIA_MAP_ZOOM, type MapBounds } from '../mapBounds'
import { OSM_ATTRIBUTION, OSM_TILE_URL } from '../mapConfig'
import { destinationPlaceLine } from '../mapDiscover'
import 'leaflet/dist/leaflet.css'

const DefaultIcon = L.icon({
  iconUrl: markerIcon,
  iconRetinaUrl: markerIcon2x,
  shadowUrl: markerShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
})
L.Marker.prototype.options.icon = DefaultIcon

function BoundsWatcher({
  onBounds,
  ignoreMoveEndRef,
}: {
  onBounds: (bounds: MapBounds) => void
  ignoreMoveEndRef: MutableRefObject<boolean>
}) {
  const map = useMapEvents({
    moveend: () => {
      if (ignoreMoveEndRef.current) {
        ignoreMoveEndRef.current = false
        return
      }
      const bounds = map.getBounds()
      onBounds({
        south: bounds.getSouth(),
        west: bounds.getWest(),
        north: bounds.getNorth(),
        east: bounds.getEast(),
      })
    },
  })

  useEffect(() => {
    const bounds = map.getBounds()
    onBounds({
      south: bounds.getSouth(),
      west: bounds.getWest(),
      north: bounds.getNorth(),
      east: bounds.getEast(),
    })
  }, [map, onBounds])

  return null
}

function SelectedFocus({
  destination,
  ignoreMoveEndRef,
}: {
  destination: Destination | null
  ignoreMoveEndRef: MutableRefObject<boolean>
}) {
  const map = useMap()
  const lastFocusedId = useRef<number | null>(null)

  useEffect(() => {
    if (!destination) {
      lastFocusedId.current = null
      return
    }
    if (lastFocusedId.current === destination.id) return
    lastFocusedId.current = destination.id
    ignoreMoveEndRef.current = true
    map.panTo([destination.latitude, destination.longitude], { animate: true })
    const clearIgnore = window.setTimeout(() => {
      ignoreMoveEndRef.current = false
    }, 500)
    return () => window.clearTimeout(clearIgnore)
  }, [destination, map, ignoreMoveEndRef])

  return null
}

export function DestinationMap({
  destinations,
  selected,
  onSelect,
  onBoundsChange,
}: {
  destinations: Destination[]
  selected: Destination | null
  onSelect: (destination: Destination) => void
  onBoundsChange: (bounds: MapBounds) => void
}) {
  const ignoreMoveEndRef = useRef(false)

  return (
    <div className="h-[min(70vh,32rem)] w-full overflow-hidden rounded-2xl border border-line" role="region" aria-label="Destination map">
      <MapContainer
        center={INDIA_MAP_CENTER}
        zoom={INDIA_MAP_ZOOM}
        className="h-full w-full"
        scrollWheelZoom
        keyboard
      >
        <TileLayer attribution={OSM_ATTRIBUTION} url={OSM_TILE_URL} />
        <BoundsWatcher onBounds={onBoundsChange} ignoreMoveEndRef={ignoreMoveEndRef} />
        <SelectedFocus destination={selected} ignoreMoveEndRef={ignoreMoveEndRef} />
        {destinations.map((destination) => (
          <Marker
            key={destination.id}
            position={[destination.latitude, destination.longitude]}
            eventHandlers={{
              click: () => onSelect(destination),
            }}
          >
            <Popup>
              <strong>{destination.name}</strong>
              <div>{destinationPlaceLine(destination)}</div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
      <p className="sr-only">
        Map tiles © OpenStreetMap contributors. Use the destination list for keyboard selection.
      </p>
    </div>
  )
}
