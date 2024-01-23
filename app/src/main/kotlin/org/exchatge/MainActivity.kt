
package org.exchatge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.exchatge.pages.LogInRegisterPage
import org.exchatge.pages.UsersListPage
import org.exchatge.ui.theme.ExchatgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            org.exchatge.Preview()
        }
    }
}

private const val CURRENT_PANEL = 0 // TODO: debug only

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Preview() = ExchatgeTheme {
    Surface(
        modifier = Modifier.fillMaxSize(), //.border(1.0f.dp, color = Color.Black, RoundedCornerShape(1.0f.dp)),
        color = MaterialTheme.colorScheme.background
    ) {
        when (CURRENT_PANEL) {
            0 -> LogInRegisterPage()
            1 -> UsersListPage()
        }
    }
}
