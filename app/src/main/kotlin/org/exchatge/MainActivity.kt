
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
        TextField(
            value = "",
            onValueChange = {},
            modifier = paddingModifier,
            label = { Text(stringResource(R.string.username)) }
        )
        TextField(
            value = "",
            onValueChange = {},
            modifier = paddingModifier,
            label = { Text(stringResource(R.string.password)) }
        )
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
