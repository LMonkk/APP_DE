package com.example.recuerdosMLJDMH

import android.content.Context
import androidx.compose.runtime.saveable.rememberSaveable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.example.recuerdosMLJDMH.ui.theme.RecuerdosTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// DataStore extension
val Context.dataStore by preferencesDataStore(name = "user_preferences")

// UserPreferences class
class UserPreferences(private val context: Context) {

    companion object {
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val IS_REGISTERED_KEY = booleanPreferencesKey("is_registered")
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
            preferences[IS_REGISTERED_KEY] = true
        }
    }

    val userName: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME_KEY] ?: ""
        }

    val isRegistered: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_REGISTERED_KEY] ?: false
        }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecuerdosTheme {
                RecuerdosApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun RecuerdosApp() {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    var isRegistered by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isRegistered = userPreferences.isRegistered.first()
        userName = userPreferences.userName.first()
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        if (!isRegistered) {
            RegistrationScreen(
                onRegisterComplete = { name ->
                    // Usamos lifecycleScope desde el contexto de la actividad
                    val activity = context as MainActivity
                    activity.lifecycleScope.launch {
                        userPreferences.saveUserName(name)
                        isRegistered = true
                        userName = name
                    }
                }
            )
        } else {
            MainAppScreen(userName = userName)
        }
    }
}

@Composable
fun RegistrationScreen(
    onRegisterComplete: (String) -> Unit
) {
    var userName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "¡Bienvenido!",
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Por favor, ingresa tu nombre para continuar",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = {
                userName = it
                isError = false
            },
            label = { Text("Tu nombre") },
            placeholder = { Text("Ej: Juan Pérez") },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text("Por favor ingresa tu nombre")
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (userName.isNotBlank()) {
                    onRegisterComplete(userName)
                } else {
                    isError = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = userName.isNotBlank()
        ) {
            Text("Comenzar")
        }
    }
}

@Composable
fun MainAppScreen(userName: String) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> Pagina1(
                    modifier = Modifier.padding(innerPadding),
                    userName = userName
                )
                AppDestinations.FAVORITES -> Pagina2(
                    modifier = Modifier.padding(innerPadding),
                    userName = userName
                )
                AppDestinations.PROFILE -> Pagina3(
                    modifier = Modifier.padding(innerPadding),
                    userName = userName
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Pacientes", Icons.Default.Home),
    FAVORITES("Recordatorios", Icons.Default.Favorite),
    PROFILE("Perfil", Icons.Default.AccountBox),
}

@Composable
fun Pagina1(
    modifier: Modifier = Modifier,
    userName: String = ""
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (userName.isNotBlank()) {
            Text(
                text = "Hola, $userName!",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = "Pacientes",
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Lista de pacientes",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun Pagina2(
    modifier: Modifier = Modifier,
    userName: String = ""
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (userName.isNotBlank()) {
            Text(
                text = "Hola, $userName!",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = "Recordatorios",
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Tus recordatorios",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun Pagina3(
    modifier: Modifier = Modifier,
    userName: String = ""
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (userName.isNotBlank()) {
            Text(
                text = "Hola, $userName!",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = "Perfil",
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Configuración de perfil",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar el nombre del usuario
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = "Usuario: $userName",
                modifier = Modifier.padding(16.dp),
                fontSize = 18.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RecuerdosTheme {
        Pagina1(userName = "Ejemplo")
    }
}