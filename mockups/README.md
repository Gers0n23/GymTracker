# Mockups de GymTracker (FitTracker)

Esta carpeta contiene los prototipos visuales (HTML/CSS) basados en el el documento **Levantamiento Tecnico.pdf**. Los diseños siguen una estética "Premium Dark Mode" con glassmorphism, optimizados para una experiencia móvil nativa (Android).

## Estructura de Pantallas y Funcionalidades

### 1. Dashboard (`dashboard.html`) - Pantalla de Inicio
*   **Resumen de Hoy:** Carga automática de la rutina del día.
*   **Estadísticas Rápidas:** Sesiones del mes y volumen de entrenamiento.
*   **Mini Gráfico de Actividad:** Visualización semanal de consistencia.
*   **Acceso Rápido:** Botón destacado para iniciar la sesión.

### 2. Sesión Activa (`active_session.html`) - Núcleo de la App
*   **Registro en Tiempo Real:** Entrada de peso y repeticiones por serie.
*   **Temporizador de Descanso:** Integrado y visible (Fase 1.2).
*   **Estado de Ejercicio:** Marca visual (checks) para series completadas.
*   **Historial Rápido:** Botón para consultar pesos anteriores del ejercicio actual.

### 3. Gestión de Rutinas (`routines.html`)
*   **Lista de Rutinas:** Visualización de días asignados y previsualización de ejercicios.
*   **Creación/Edición:** Preparado para el flujo de gestión de rutinas (Fase 1.1).

### 4. Progreso y Logros (`progression.html`)
*   **Gráficos de Fuerza:** Evolución de peso a lo largo del tiempo (Fase 1.4).
*   **Alertas de Plateau:** Notificaciones visuales cuando no hay progreso (Fase 1.4).
*   **Gamificación:** Sistema de racha (streak) e hitos conseguidos.

### 5. Métricas Corporales (`body_metrics.html`) - Fase 2
*   **Control de Peso:** Registro y tendencia semanal.
*   **Composición:** Porcentaje de grasa (Navy method) y medidas de perímetros.

## Consideraciones Técnicas (Stack sugerido en PDF)
*   **Frontend:** Kotlin + Jetpack Compose.
*   **Base de datos:** Room (SQLite).
*   **Gráficos:** Vico o MPAndroidChart.
*   **IA:** Integración futura con Claude/OpenAI para análisis de patrones.
