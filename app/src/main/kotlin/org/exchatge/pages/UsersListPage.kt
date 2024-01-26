
package org.exchatge.pages

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.vector.ImageVector
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
        modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserInfo(
    id: Int,
    name: String,
    online: Boolean,
    conversationExists: Boolean
) {
    val fontSize = 16.sp

    Box(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = {})) {
        ListItem(
            leadingContent = {
                Text(
                    text = id.toString(),
                    fontSize = fontSize,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            headlineContent = {
                Text(
                    text = name,
                    fontSize = fontSize,
                    fontWeight = if (!conversationExists) FontWeight.Normal else FontWeight.Bold
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(if (online) R.string.online else R.string.offline),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        )
    }
}
