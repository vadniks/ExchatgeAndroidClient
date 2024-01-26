
package org.exchatge.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exchatge.R

private val paddingModifier = Modifier.padding(2.5f.dp)

private var username by mutableStateOf("")
private var password by mutableStateOf("")
private var autoLoggingIn by mutableStateOf(false)

@Composable
fun LogInRegisterPage() = Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.75f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.appName),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = paddingModifier
        )
        TextField(username, R.string.username, false) { username = it }
        TextField(password, R.string.password, true) { password = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.autoLoggingIn))
            Checkbox(
                checked = autoLoggingIn,
                onCheckedChange = { autoLoggingIn = it },
                modifier = Modifier.alignByBaseline()
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {},
                modifier = paddingModifier
            ) {
                Text(stringResource(R.string.logIn))
            }
            Button(
                onClick = {},
                modifier = paddingModifier
            ) {
                Text(stringResource(R.string.register))
            }
        }
    }
}

@Composable
private fun TextField(
    value: String,
    label: Int,
    password: Boolean,
    onValueChange: (String) -> Unit
) = TextField(
    value = value,
    onValueChange = onValueChange,
    modifier = paddingModifier.fillMaxWidth(),
    label = { Text(stringResource(label)) },
    singleLine = true,
    visualTransformation = if (!password) VisualTransformation.None else PasswordVisualTransformation()
)
