
package org.exchatge.pages

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        }
    }
}
