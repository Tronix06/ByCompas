# ByCompas 🧭🏃‍♂️
> El radar multideporte definitivo para conectar a deportistas locales. Encuentra compañeros, organiza eventos y no vuelvas a quedarte en el banquillo.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/firebase-%23039BE5.svg?style=for-the-badge&logo=firebase)
![Google Maps](https://img.shields.io/badge/Google_Maps-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white)

<p align="center">
  <img src="ruta/a/tu/captura_home.png" alt="Pantalla Principal del Radar" width="250">
</p>

## 📖 El Problema que resolvemos
Actualmente, organizar un partido de pádel, una pachanga de fútbol o encontrar un compañero de *running* con tu mismo ritmo es una odisea que se pierde en grupos de WhatsApp saturados. 

**ByCompas** soluciona este problema mediante un mapa interactivo (Radar) que geolocaliza eventos deportivos cercanos en tiempo real, permitiendo a los usuarios filtrar por deporte, nivel exigido y distancia.

## 🚀 Funcionalidades Principales (MVP)

- 📍 **Radar en Tiempo Real:** Un mapa interactivo impulsado por Google Maps SDK que detecta tu ubicación y muestra "chinchetas" con los eventos creados en tu radio de acción (configurable de 1km a 50km).
- 🎾 **Perfiles Multideporte Dinámicos:** Los usuarios configuran su nivel de forma específica para cada deporte (Ej: *Fútbol - Amateur*, *Running - < 5:00 min/km*).
- 🏟️ **Creación Precisa de Eventos:** Formulario de creación de actividades con un mini-mapa interactivo para fijar el punto de encuentro exacto mediante coordenadas GPS (Lat/Lng), fecha, hora y plazas libres.
- 👥 **Sistema de Matchmaking ("El Vestuario"):** Flujo de solicitudes donde los usuarios piden unirse a un evento y el organizador los acepta o rechaza basándose en su nivel y reputación (estrellas).
- 🔄 **Vistas Dinámicas:** Alternancia fluida entre "Vista de Mapa" y "Vista de Lista" generada dinámicamente mediante consultas en tiempo real a la base de datos.

## 🛠️ Arquitectura y Stack Tecnológico

ByCompas ha sido desarrollada bajo una arquitectura moderna y escalable, utilizando las últimas herramientas del ecosistema Android:

### Frontend (Móvil Nativo) 📱
- **Lenguaje:** Kotlin.
- **UI Toolkit:** Jetpack Compose (100% UI Declarativa).
- **Navegación:** Jetpack Navigation Compose con paso de argumentos.
- **Hardware/Sensores:** Integración de Google Location Services para tracking GPS preciso.
- **Mapas:** Librería oficial `maps-compose` para la renderización de mapas y marcadores directamente desde el estado de la UI.

### Backend as a Service (BaaS) ☁️
- **Autenticación:** Firebase Auth (Email/Contraseña y flujos de recuperación).
- **Base de Datos:** Cloud Firestore. Base de datos NoSQL en tiempo real. Uso de colecciones distribuidas (`users`, `events`) y `SnapshotListeners` para actualizar la UI del mapa sin necesidad de recargar la pantalla.

## 🌟 ¿Por qué ByCompas destaca?

- **UX de Nivel Producción:** Diseño cuidado con *BottomSheets*, validación de datos en vivo, selectores nativos de Android y "Map Padding" para evitar la colisión de la interfaz con los controles de Google Maps.
- **Escalabilidad Deportiva:** La estructura de datos permite añadir nuevos deportes y métricas (niveles o ritmos) sin tener que alterar el código del frontend, gracias a su diseño dinámico.
- **Gestión Inteligente de Permisos:** Flujo de *Onboarding* amigable que educa al usuario antes de solicitar permisos críticos (Ubicación y Notificaciones).

---
*Proyecto desarrollado como Trabajo de Fin de Grado (TFG).*
