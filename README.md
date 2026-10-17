# Reto2

Es una app Android desarrollada con Compose y MVVM donde el usuario inicia sesión mediante una base de datos Room, consulta tasas de cambio a través de Retrofit y simula operaciones de compra o venta de USD. Cada transacción registra la tasa utilizada, y si la variación supera el 2,5 % respecto a la última operación, se marca automáticamente para revisión en el panel de Auditoría.

## Arquitectura y Tecnologías
El proyecto sigue el patrón MVVM y un flujo de datos unidireccional (Pantalla → ViewModel → Repositorio/DAO → Base de datos/Red → StateFlow → Pantalla):
- **Interfaz de Usuario (Compose):** LoginScreen, RegisterScreen, OperationsScreen, ProfileScreen, AdminAuditScreen. El diseño utiliza un tema central (`Theme.kt`) con fondo gris oscuro (`0xFF2B2E33`), detalles en azul (`0xFF8AB4F8`) y tarjetas en gris claro.
- **ViewModels:** AuthViewModel, OperationsViewModel, ProfileViewModel, AdminAuditViewModel.
- **Repositorios:** TransactionRepository (lógica de operaciones), RatesRepository (consultas de red).
- **Base de Datos (Room):** `AppDatabase` con `UserEntity` y `TransactionEntity`. El inicio (`seedCallback`) genera usuarios y transacciones por defecto.
- **Red (Retrofit):** `ErApiService` gestiona la consulta de tasas de cambio en tiempo real.
- **Navegación:** Gestionada mediante `NavGraph` con la ruta de inicio en `login`.

## Estructura de Base de Datos
- **Usuarios (`users`):** Almacena `id`, `email` (único), `password` (hasheado), `role` (Admin/User), `balanceUsd` (10000 por defecto) y `photoPath`.
- **Transacciones (`transactions`):** Registra `userId`, `currencyPair` (ej. USD/COP), `amount` (en USD), `currentExchangeRate`, `transactionType` (Compra/Venta), `timestamp`, `requiresAudit` y `deviationPercent`.

## Pantallas Principales

### 1. Autenticación (Login y Registro)
- Formulario de ingreso con campos para correo y contraseña sobre un fondo gris oscuro.
- Las validaciones comprueban que no haya campos vacíos, el formato del correo, contraseñas de al menos 6 caracteres y coincidencias en la confirmación. 
- La sesión se maneja en memoria (`SessionManager.currentUser`). Si la sesión se pierde, la aplicación redirige al login. Al iniciar sesión, la pila de navegación se limpia para evitar regresar a esta pantalla.

<p align="center">
  <img src="https://github.com/user-attachments/assets/6329d96f-1b66-46a3-b424-39acc1fe0936" width="45%" />
  <img src="https://github.com/user-attachments/assets/f2ae9d5d-a4c7-4660-b175-b4623b165297" width="45%" />
</p>

### 2. Operaciones (Pantalla Principal de Usuarios)
- **Selección de Divisas:** Chips superiores para elegir COP, EUR, GBP o JPY. Al tocar uno, se solicita la tasa a través de Retrofit.
- **Tasa de Referencia:** Muestra el valor consultado en internet, incluye protección contra respuestas tardías y un botón para actualizar manualmente.
- **Ejecución:** Permite ingresar cantidades con decimales. Antes de guardar, se verifica que haya balance suficiente (en ventas). La operación de base de datos y la actualización del balance se hacen bajo una única transacción de Room.
- **Historial ("Mis operaciones"):** Lista reactiva que muestra transacciones con estado `SUCCESS` o `AUDITORÍA` en rojo si la desviación superó el 2,5 %.

<p align="center">
  <img src="https://github.com/user-attachments/assets/c3434e2b-185f-47e5-81e2-c0f298900dbf" width="45%" />
  <img src="https://github.com/user-attachments/assets/62242ab0-6cad-44d5-a622-77fa5d76987b" width="45%" />
</p>

### 3. Perfil
- Interfaz que muestra el correo del usuario, el balance simulado de 10000.00 USD actualizándose en tiempo real mediante un flujo reactivo, y botones para cambiar foto o volver a operaciones.
- La función de foto usa el selector del sistema, guarda la imagen internamente y actualiza la ruta en Room.

<p align="center">
  <img src="https://github.com/user-attachments/assets/c8487e3d-887c-4d00-aea7-1975bcd8b6e0" width="45%" />
  <img src="https://github.com/user-attachments/assets/486a8a75-8209-4344-8c35-79d5bab25d15" width="45%" />
</p>

### 4. Auditoría (Exclusivo Administradores)
- Panel que enumera todas las transacciones de los usuarios y cuenta cuántas requieren auditoría en la parte superior.
- Las transacciones marcadas por desviación mayor al 2,5 % (calculada frente a la tasa anterior guardada) resaltan con un fondo rojo claro y un aviso de "⚠ Requiere auditoría".
- Muestra el correo del usuario implicado y el porcentaje de desviación con dos decimales. Bloquea el acceso a usuarios sin rol de administrador.

<p align="center">
  <img src="https://github.com/user-attachments/assets/ab6a6fba-402d-4a07-8c15-863d5e1568cf" width="45%" />
  <img src="https://github.com/user-attachments/assets/c09537ee-c7dd-4a68-be94-fdc9e189cb4f" width="45%" />
</p>









