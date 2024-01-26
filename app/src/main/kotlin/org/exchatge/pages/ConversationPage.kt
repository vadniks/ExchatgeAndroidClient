
package org.exchatge.pages

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exchatge.R
import java.text.SimpleDateFormat

private val opponentUsername = "User" // TODO: debug only

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationPage() = Scaffold(
    topBar = {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.appName))
                    Text(
                        text = opponentUsername,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )
    }
) { paddingValues ->
    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
        items(10) { // TODO: debug only
            Message(System.currentTimeMillis(), if (it % 2 == 0) "User$it" else null, "Text$it")
        }
    }
}

@SuppressLint("SimpleDateFormat")
@Composable
private fun Message(timestamp: Long, from: String?, text: String) = Box(
    contentAlignment = if (from != null) Alignment.CenterStart else Alignment.CenterEnd,
    modifier = Modifier.fillMaxWidth().padding(5.dp)
) {
    Column(
        modifier = Modifier.fillMaxWidth(.48f),
        horizontalAlignment = if (from != null) Alignment.Start else Alignment.End
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Justify
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = SimpleDateFormat("hh:mm:ss MMM-DD-yyyy").format(timestamp),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(end = 5.dp)
            )
            Text(
                text = from ?: stringResource(R.string.you),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 5.dp)
            )
        }
    }
}
