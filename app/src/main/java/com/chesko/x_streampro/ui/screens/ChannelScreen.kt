package com.chesko.x_streampro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chesko.x_streampro.R
import com.chesko.x_streampro.data.XtreamRepository
import com.chesko.x_streampro.data.model.LiveStream
import com.chesko.x_streampro.data.model.UserSession
import com.chesko.x_streampro.ui.theme.XStreamProTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    session: UserSession,
    categoryId: String,
    categoryName: String,
    onBack: () -> Unit,
    onChannelClick: (LiveStream, List<LiveStream>) -> Unit
) {
    val repository = remember { XtreamRepository() }
    var channels by remember { mutableStateOf<List<LiveStream>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(categoryId) {
        try {
            channels = repository.getLiveStreams(session.baseUrl, session.username, session.password, categoryId)
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            categoryName.uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                        )
                        Text(
                            "${channels.size} ITEMS",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back", 
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else if (errorMessage != null) {
                Text(
                    text = "Error loading channels",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (channels.isEmpty()) {
                Text(
                    text = "No channels found in this category",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(channels) { channel ->
                        ChannelGridItem(
                            channel = channel,
                            onClick = { onChannelClick(channel, channels) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelGridItem(channel: LiveStream, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.04f))
        ) {
            AsyncImage(
                model = channel.streamIcon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(6.dp),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.app_icon_android),
                error = painterResource(R.drawable.app_icon_android)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = channel.name ?: "",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 11.sp
            ),
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChannelScreenPreview() {
    XStreamProTheme {
        ChannelScreen(
            session = UserSession("http://test.com", "user", "pass"),
            categoryId = "1",
            categoryName = "Movies",
            onBack = {},
            onChannelClick = { _, _ -> }
        )
    }
}
