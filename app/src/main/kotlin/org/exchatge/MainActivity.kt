
package org.exchatge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.exchatge.pages.ConversationPage
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

var currentPage by mutableIntStateOf(1) // TODO: debug only

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Preview() = ExchatgeTheme/*(darkTheme = true)*/ {
    Surface(
        modifier = Modifier.fillMaxSize(), //.border(1.0f.dp, color = Color.Black, RoundedCornerShape(1.0f.dp)),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentPage) {
            0 -> LogInRegisterPage()
            1 -> UsersListPage()
            2 -> ConversationPage()
        }
    }
}
