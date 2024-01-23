
package org.exchatge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exchatge.ui.theme.ExchatgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            org.exchatge.Preview()
        }
    }
}

@Composable
fun LogInRegisterPanel() {
    val paddingModifier = Modifier.padding(2.5f.dp)

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    @Composable
    fun textField(
        value: String,
        label: Int,
        password: Boolean,
        onValueChange: (String) -> Unit
    ) {
        return TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = paddingModifier,
            label = { Text(stringResource(label)) },
            singleLine = true,
            visualTransformation =
                if (!password) VisualTransformation.None
                else PasswordVisualTransformation()
        )
    }

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
        textField(username, R.string.username, false) { username = it }
        textField(password, R.string.password, true) { password = it }
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Preview() {
    ExchatgeTheme(true) {
        Surface(
            modifier = Modifier.fillMaxSize(), //.border(1.0f.dp, color = Color.Black, RoundedCornerShape(1.0f.dp)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogInRegisterPanel()
            }
        }
    }
}
