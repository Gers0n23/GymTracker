# GymTracker (FitTracker) - Documentación de Avances y Planificación

Este repositorio contiene la fase de diseño y prototipado de **GymTracker**, una aplicación Android premium diseñada para el seguimiento avanzado de entrenamientos de fuerza y composición corporal.

## 🚀 Avances Actuales (Mockups)

Se han desarrollado 7 pantallas interactivas que definen el flujo de usuario solicitado:

1.  **Dashboard (`dashboard.html`)**: Vista general con resumen de actividad y acceso rápido.
2.  **Gestión de Rutinas (`routines.html`)**: Listado de entrenamientos programados.
3.  **Previsualización de Rutina (`routine_preview.html`)**: Carga los ejercicios y los **últimos pesos/reps realizados** antes de iniciar.
4.  **Sesión Activa Avanzada (`timer_session.html` & `active_session.html`)**: 
    *   Registro de **RIR (Reps in Reserve)** por cada serie para autoregulación.
    *   Detección y visualización de **Personal Bests (PR)** dinámicos en tiempo real.
    *   Cronómetros de Serie/Descanso y seguimiento de Tiempo Total.
5.  **Dashboard de Inteligencia Corporal (`dashboard.html` & `body_metrics.html`)**:
    *   **Mapa de Calor de Recuperación**: Visualización anatómica del estado muscular basado en entrenamiento previo.
    *   **Volumen por Grupo Muscular**: Gráficos automáticos de carga de trabajo semanal.
    *   Cálculo automático de IMC y tendencias de composición.
6.  **Autoregulación Pre-Entreno (`routine_preview.html`)**: Cuestionario "Ready to Train" para ajustar la intensidad basada en sueño y estrés.
7.  **Logros (`progression.html`)**: Visualización de hitos y alertas de estancamiento.

---

## 🏗️ Estructura Técnica (Android Kotlin)

Para llevar estos mockups a una aplicación nativa, utilizaremos la siguiente arquitectura moderna:

### 1. Arquitectura de Software
*   **Patrón:** MVVM (Model-View-ViewModel) con Clean Architecture.
*   **UI:** Jetpack Compose (Declarativa, replicando el diseño CSS actual).
*   **Navegación:** Compose Navigation con rutas Type-Safe.
*   **Inyección de Dependencias:** Hilt o Koin.

### 2. Capa de Datos (Room Database)
*   **`User`**: Perfil, altura (para IMC), preferencias.
*   **`Routine`**: ID, Nombre, Días asignados.
*   **`Exercise`**: ID, Nombre, Máquina, ID_Rutina.
*   **`SetRecord`**: ID_Ejercicio, Peso, Reps, Timestamp, Duración_Serie.
*   **`BodyMetric`**: Fecha, Peso, %Grasa, %Músculo, IMC.

### 3. Lógica de Negocio (Use Cases)
*   `GetLastExercisePerformance`: Busca en la DB el último registro de un ejercicio para el preview.
*   `CalculateIMC`: Lógica para procesar peso/altura.
*   `DetectPlateau`: Algoritmo que compara las últimas 3 sesiones para alertar estancamiento.

---

## 🏗️ Estado Actual del Proyecto
- [x] **Cimientos**: Proyecto Android inicializado con Jetpack Compose.
- [x] **Persistencia Híbrida**: Integración con Firebase Firestore con soporte **Offline First** habilitado (sincronización automática).
- [x] **Arquitectura**: Modelos de dominio implementados (User, Routine, Exercise, Workout, BodyMetrics).
- [x] **Diseño Premium**: Sistema de temas "Glassmorphism" y colores basados en mockups.
- [x] **Navegación**: NavHost configurado con rutas básicas.
- [x] **Dashboard**: Pantalla principal funcional con estadísticas rápidas y diseño moderno.

---

## 🗺️ Roadmap de Desarrollo Actualizado

### Fase 1: Motor de Datos y Rutinas (COMPLETADA)
- [x] **Gestión de Rutinas**: Pantalla para listar y crear rutinas desde Firebase.
- [x] **Repositorios**: Implementar la lógica para leer/escribir en Firestore.
- [x] **Visualización de Ejercicios**: Detalle de cada rutina con su lista de ejercicios.

### Fase 2: El Corazón del Entrenamiento (COMPLETADA)
- [x] **Sesión Activa**: Cronómetro funcional y registro de series en tiempo real.
- [x] **Lógica de RPE/RIR**: Selector interactivo de esfuerzo percibido.
- [ ] **Detección de PRs**: Alertas instantáneas al romper récords personales (Pendiente lógica de comparación).

### Fase 3: Análisis y Biometría (EN PROCESO)
- [ ] **Mapa de Calor Anatómico**: Implementación visual de la recuperación muscular en el Dashboard.
- [ ] **Gráficos de Evolución**: Integración de la librería Vico para volumen de carga y peso.
- [x] **Seguimiento Corporal**: Pantalla de ingreso de peso/parámetros con cálculo de IMC.

---

## ⚡ Notas para la Próxima Sesión
1.  **Firebase Firestore**: Configurar las reglas de seguridad en la consola de Firebase.
2.  **Pantalla de Rutinas**: Traducir `routines.html` a Compose.
3.  **Lógica Offline**: Evaluar si añadimos una capa de caché local para cuando no haya internet.
