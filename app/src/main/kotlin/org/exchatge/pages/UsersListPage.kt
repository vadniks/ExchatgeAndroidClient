
package org.exchatge.pages

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.exchatge.R

private val currentUser = "User" // TODO: debug only
private val admin = true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersListPage() = Scaffold(
    topBar = {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.appName))
                    Text(
                        text = currentUser,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        fontWeight = if (!admin) FontWeight.Normal else FontWeight.Bold
                    )
                }
            },
            colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = stringResource(R.string.logOut)
                    )
                }
            },
            actions = {
                if (admin) IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = stringResource(R.string.administrate)
                    )
                }
            }
        )
    }
) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyColumn {
                items(10) { // TODO: debug only
                    UserInfo(
                        id = it,
                        name = "User$it",
                        online = it % 2 == 0,
                        conversationExists = it % 3 == 0
                    )
                }
            }
        }
    }
}

@Composable
private fun UserInfo(
    id: Int,
    name: String,
    online: Boolean,
    conversationExists: Boolean
) {
    val fontSize = 16.sp

    ListItem(
        leadingContent = {
            Text(
                text = id.toString(),
                fontSize = fontSize,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth(.1f)
            )
        },
        headlineContent = {
            Text(
                text = name,
                fontSize = fontSize,
                modifier = Modifier.fillMaxWidth(.5f)
            )
        },
        supportingContent = {
            Text(
                text = stringResource(if (online) R.string.online else R.string.offline),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth(.5f)
            )
        },
        trailingContent = {
            @Composable
            fun Content() {
                @Composable
                fun Button(text: Int) = TextButton(onClick = {}) {
                    Text(
                        text = stringResource(text),
                        fontSize = 12.sp
                    )
                }

                if (!conversationExists)
                    Button(R.string.startConversation)
                else {
                    Button(R.string.continueConversation)
                    Button(R.string.deleteConversation)
                }
            }

            val modifier = Modifier.fillMaxWidth(.5f)
            val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

            if (portrait) Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                content = { Content() }
            ) else Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.Center,
                content = { Content() }
            )
        }
    )
}
