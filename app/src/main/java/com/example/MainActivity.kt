package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.BloggerTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: BloggerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsState by viewModel.settings.collectAsState()
            
            BloggerTheme(
                themeName = settingsState.themeName,
                isDarkMode = settingsState.isDarkMode
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeContent
                ) { innerPadding ->
                    BloggerMainScreen(
                        viewModel = viewModel,
                        settings = settingsState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Custom Helper to format Date timestamps neatly
fun Long.toDateString(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}

@Composable
fun BloggerMainScreen(
    viewModel: BloggerViewModel,
    settings: SiteSettings,
    modifier: Modifier = Modifier
) {
    val activeTabState by viewModel.activeTab.collectAsState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    val isNewPostOpen by viewModel.isCreatingNewPost.collectAsState()
    val isNewPageOpen by viewModel.isCreatingNewPage.collectAsState()
    val postToEdit by viewModel.selectedPostForEdit.collectAsState()
    val pageToEdit by viewModel.selectedPageForEdit.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isTablet) {
            // Adaptive Wide-Screen Layout (Sidebar Navigation)
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRailContainer(
                    selectedTab = activeTabState,
                    onTabSelected = { viewModel.selectTab(it) },
                    settings = settings
                )
                VerticalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    ContentPane(viewModel, activeTabState, settings)
                }
            }
        } else {
            // Compact Mobile Portrait Layout (Bottom Navigation)
            Column(modifier = Modifier.fillMaxSize()) {
                // Compact App Brand Header
                BloggerMobileHeader(settings = settings, viewModel = viewModel)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    ContentPane(viewModel, activeTabState, settings)
                }
                BottomNavigationBarContainer(
                    selectedTab = activeTabState,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
        }

        // Action Overlay Drawers/Dialogs for CRUD Operations
        if (isNewPostOpen) {
            PostEditorDialog(
                post = null,
                onDismiss = { viewModel.isCreatingNewPost.value = false },
                onSave = { title, content, cat, tags, status, url ->
                    viewModel.addPost(title, content, cat, tags, status, url)
                },
                viewModel = viewModel
            )
        }

        postToEdit?.let { post ->
            PostEditorDialog(
                post = post,
                onDismiss = { viewModel.selectedPostForEdit.value = null },
                onSave = { title, content, cat, tags, status, url ->
                    viewModel.updatePost(post.id, title, content, cat, tags, status, url, post.views)
                },
                viewModel = viewModel
            )
        }

        if (isNewPageOpen) {
            PageEditorDialog(
                page = null,
                onDismiss = { viewModel.isCreatingNewPage.value = false },
                onSave = { title, content, slug ->
                    viewModel.addPage(title, content, slug)
                }
            )
        }

        pageToEdit?.let { page ->
            PageEditorDialog(
                page = page,
                onDismiss = { viewModel.selectedPageForEdit.value = null },
                onSave = { title, content, slug ->
                    viewModel.updatePage(page.id, title, content, slug, page.timestamp)
                }
            )
        }
    }
}

// --- App Layout Subcomponents ---

@Composable
fun BloggerMobileHeader(settings: SiteSettings, viewModel: BloggerViewModel) {
    val liveVisitors by viewModel.simulatedLiveVisitors.collectAsState()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = settings.blogTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (settings.customDomain.isNotEmpty()) settings.customDomain else "bloggerclone.site",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            // Real-Time Animated Pulse Active Visitors
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable { viewModel.refreshLiveAnalytics() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Green, shape = CircleShape)
                    )
                    Text(
                        text = "$liveVisitors Live",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationRailContainer(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    settings: SiteSettings
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = settings.blogTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 100.dp)
                )
            }
        },
        modifier = Modifier.testTag("admin_sidebar")
    ) {
        val menuItems = listOf(
            "Posts" to Icons.Default.List,
            "Stats" to Icons.Default.TrendingUp,
            "Comments" to Icons.Default.Comment,
            "Earnings" to Icons.Default.AccountBalanceWallet,
            "Pages" to Icons.Default.Description,
            "Theme" to Icons.Default.Palette,
            "AI Toolkit" to Icons.Default.Star,
            "Settings" to Icons.Default.Settings
        )

        menuItems.forEach { (title, icon) ->
            NavigationRailItem(
                selected = selectedTab == title,
                onClick = { onTabSelected(title) },
                icon = { Icon(imageVector = icon, contentDescription = title) },
                label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun BottomNavigationBarContainer(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp
    ) {
        val menuItems = listOf(
            "Posts" to Icons.Default.List,
            "Stats" to Icons.Default.TrendingUp,
            "Comments" to Icons.Default.Comment,
            "Earnings" to Icons.Default.AccountBalanceWallet,
            "Pages" to Icons.Default.Description,
            "Theme" to Icons.Default.Palette,
            "AI Toolkit" to Icons.Default.Star,
            "Settings" to Icons.Default.Settings
        )

        menuItems.forEach { (title, icon) ->
            NavigationBarItem(
                selected = selectedTab == title,
                onClick = { onTabSelected(title) },
                icon = { Icon(imageVector = icon, contentDescription = title) },
                label = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                ),
                modifier = Modifier.testTag("bottom_nav_$title")
            )
        }
    }
}

@Composable
fun ContentPane(
    viewModel: BloggerViewModel,
    tabName: String,
    settings: SiteSettings
) {
    AnimatedContent(
        targetState = tabName,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "ContentPanelTransition"
    ) { currentTab ->
        when (currentTab) {
            "Posts" -> PostsTabScreen(viewModel)
            "Stats" -> StatsTabScreen(viewModel)
            "Comments" -> CommentsTabScreen(viewModel)
            "Earnings" -> EarningsTabScreen(viewModel, settings)
            "Pages" -> PagesTabScreen(viewModel)
            "Theme" -> ThemeTabScreen(viewModel, settings)
            "AI Toolkit" -> AiToolkitScreen(viewModel)
            "Settings" -> SettingsTabScreen(viewModel, settings)
            else -> PostsTabScreen(viewModel)
        }
    }
}

// --- Target Tab Screens ---

@Composable
fun PostsTabScreen(viewModel: BloggerViewModel) {
    val postsList by viewModel.allPosts.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Posts Header & Filter Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Blogger Posts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Write drafts, publish, or configure scheduling.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            FloatingActionButton(
                onClick = { viewModel.isCreatingNewPost.value = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_post_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Post")
            }
        }

        // Dynamic categories tags horizontal flow
        val availableCategories = listOf("All") + postsList.map { it.category }.distinct()
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(availableCategories) { cat ->
                val isSelected = selectedCategoryFilter == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Post list viewport
        val filteredPosts = if (selectedCategoryFilter == "All") postsList 
                            else postsList.filter { it.category == selectedCategoryFilter }

        if (filteredPosts.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "No Posts",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "No blog posts found.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Text(
                        "Click the '+' button to write your first post!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredPosts, key = { it.id }) { post ->
                    PostRowCard(
                        post = post,
                        onClick = { viewModel.selectedPostForEdit.value = post },
                        onDelete = { viewModel.deletePost(post.id) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun PostRowCard(
    post: BlogPost,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    viewModel: BloggerViewModel
) {
    var expandedComments by remember { mutableStateOf(false) }
    var userCommentAuthor by remember { mutableStateOf("") }
    var userCommentBody by remember { mutableStateOf("") }
    val postComments by viewModel.getCommentsForPost(post.id).collectAsState(initial = emptyList())
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Post Metadata Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(post.category, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    
                    val statusColor = if (post.status == "Published") Color(0xFF2E7D32) else Color(0xFFC62828)
                    Text(
                        text = post.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onClick) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Post", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Post", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Body Display Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (post.thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model = post.thumbnailUrl,
                        contentDescription = "Post Cover Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Published: ${post.publishTime.toDateString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Truncated Preview of Content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            // Tags List Badges
            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    post.tags.split(",").forEach { tag ->
                        Text(
                            text = "#${tag.trim()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            // Post Stats Footer & Comments Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.Default.TrendingUp, contentDescription = "Views", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("${post.views} Views", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Row(
                        modifier = Modifier.clickable { expandedComments = !expandedComments },
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Comment, contentDescription = "Comments", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(
                            "${postComments.size} Comments", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Demo Share Switch
                IconButton(onClick = { Toast.makeText(context, "Link copied to clipboard!\n${post.title}", Toast.LENGTH_SHORT).show() }) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }

            // Expanded comments section inside the post row
            if (expandedComments) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Reader Discussion:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (postComments.isEmpty()) {
                        Text(
                            "No comments yet. Be the first to express your thoughts!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        postComments.forEach { comment ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = comment.author,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = comment.timestamp.toDateString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                                Text(
                                    text = comment.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            }
                        }
                    }

                    // Leave quick mock comment form
                    Text("Express your opinion:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = userCommentAuthor,
                        onValueChange = { userCommentAuthor = it },
                        placeholder = { Text("Your name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = userCommentBody,
                        onValueChange = { userCommentBody = it },
                        placeholder = { Text("Write your comment here...") },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        maxLines = 2
                    )
                    Button(
                        onClick = {
                            if (userCommentAuthor.isNotEmpty() && userCommentBody.isNotEmpty()) {
                                viewModel.addComment(post.id, userCommentAuthor, userCommentBody)
                                userCommentAuthor = ""
                                userCommentBody = ""
                            } else {
                                Toast.makeText(context, "Fill both fields to post comment!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Submit Comment", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsTabScreen(viewModel: BloggerViewModel) {
    val liveVisitors by viewModel.simulatedLiveVisitors.collectAsState()
    val totalDailyHits by viewModel.simulatedDailyHits.collectAsState()
    val isRefreshing by viewModel.isSimulatingState.collectAsState()
    val postsList by viewModel.allPosts.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Title bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Blogger Dashboard Stats",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Live metrics, reader devices, and search indicators.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            IconButton(
                onClick = { viewModel.refreshLiveAnalytics() },
                enabled = !isRefreshing,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Stats", tint = Color.White)
                }
            }
        }

        // Summary Core KPIs row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Live Visitors", style = MaterialTheme.typography.labelSmall)
                    Text("$liveVisitors active", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Refreshing live metrics...", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Page Views", style = MaterialTheme.typography.labelSmall)
                    Text("$totalDailyHits today", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("+14.2% from yesterday", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // Beautiful custom Canvas graph explaining traffic stream
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Traffic Flow (Hour-by-Hour)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Custom drawn Line / Area Traffic Graph
                val strokeColor = MaterialTheme.colorScheme.primary
                val areaColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val points = listOf(20f, 45f, 35f, 65f, 50f, 85f, liveVisitors.toFloat() * 1.5f, 120f)
                    val widthSpacing = size.width / (points.size - 1)
                    val maxHeight = size.height
                    
                    val path = Path()
                    val areaPath = Path()
                    
                    points.forEachIndexed { idx, valP ->
                        val x = idx * widthSpacing
                        val y = maxHeight - (valP / 150f) * maxHeight
                        if (idx == 0) {
                            path.moveTo(x, y)
                            areaPath.moveTo(x, maxHeight)
                            areaPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            areaPath.lineTo(x, y)
                        }
                    }
                    areaPath.lineTo(size.width, maxHeight)
                    areaPath.close()
                    
                    drawPath(areaPath, color = areaColor)
                    drawPath(path, color = strokeColor, style = Stroke(width = 3.dp.toPx()))
                    
                    // Draw grid reference bottom line
                    drawLine(
                        color = strokeColor.copy(alpha = 0.2f),
                        start = Offset(0f, maxHeight),
                        end = Offset(size.width, maxHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("10:00 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("12:00 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("NOW (Live)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Row of country breakdown stats & Devices stats
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Readers by Country", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    
                    val countries = listOf("Bangladesh" to "42%", "USA" to "24%", "India" to "18%", "UK" to "10%", "Others" to "6%")
                    countries.forEach { (country, ratio) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(country, style = MaterialTheme.typography.labelSmall)
                            Text(ratio, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(
                            progress = { ratio.replace("%", "").toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Device Layout Analytics", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    
                    val devices = listOf("Mobile Android" to "65%", "Desktop Chrome" to "22%", "Mobile iOS" to "10%", "Tablets Or DeX" to "3%")
                    devices.forEach { (device, ratio) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(device, style = MaterialTheme.typography.labelSmall)
                            Text(ratio, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        LinearProgressIndicator(
                            progress = { ratio.replace("%", "").toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentsTabScreen(viewModel: BloggerViewModel) {
    val commentsList by viewModel.allComments.collectAsState()
    val postsList by viewModel.allPosts.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Comment Moderation",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Moderate reader voices, approve, or delete comments securely.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        if (commentsList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No comments found in this blog site.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(commentsList) { comment ->
                    val matchingPost = postsList.find { it.id == comment.postId }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = comment.author.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(comment.author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    IconButton(
                                        onClick = { viewModel.deleteComment(comment.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Comment", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text(
                                    text = "Post: ${matchingPost?.title ?: "Unknown Post"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(comment.content, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(comment.timestamp.toDateString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { Toast.makeText(viewModel.getApplication(), "Comment Approved successfully", Toast.LENGTH_SHORT).show() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Approve", fontSize = 10.sp, color = Color.White)
                                    }
                                    
                                    OutlinedButton(
                                        onClick = { viewModel.deleteComment(comment.id) },
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Spam Flag", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EarningsTabScreen(viewModel: BloggerViewModel, settings: SiteSettings) {
    val balance by viewModel.simulatedMockBalance.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "AdSense & Earnings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Track your estimated revenue from Google AdSense banners and sponsorships.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // Simulated Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AdSense Estimated Balance", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium)
                    Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "AdSense", tint = Color.Yellow)
                }
                
                Text(
                    text = if (settings.monetizationEnabled) "$${String.format(Locale.US, "%.2f", balance)}" else "- Monetization Disabled -",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )

                if (settings.monetizationEnabled) {
                    Text(
                        "AdSense Publisher Key: ${if(settings.adSenseCode.isNotEmpty()) settings.adSenseCode else "Add under Config settings"}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        "Enable Google AdSense in Settings to start displaying mock ads and generating revenue stream visualizations.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Estimated Earnings Graph Custom Paint
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Daily Ad Earnings Velocity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(10.dp))
                
                Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    val points = listOf(10f, 15f, 8f, 22f, 30f, 45f, 38f)
                    val space = size.width / (points.size - 1)
                    val stroke = Stroke(width = 3.dp.toPx())
                    val graphPath = Path()
                    
                    points.forEachIndexed { i, p ->
                        val x = i * space
                        val y = size.height - (p / 60f) * size.height
                        if (i == 0) graphPath.moveTo(x, y) else graphPath.lineTo(x, y)
                    }
                    drawPath(graphPath, color = Color(0xFFFFB300), style = stroke)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mon", style = MaterialTheme.typography.labelSmall)
                    Text("Wed", style = MaterialTheme.typography.labelSmall)
                    Text("Today", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Mock Ad Template Previews
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Simulated Ad Layout Config", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Header Banner Ads", fontSize = 13.sp)
                    Switch(checked = settings.monetizationEnabled, onCheckedChange = { 
                        Toast.makeText(context, "Header Banner is enabled on published posts!", Toast.LENGTH_SHORT).show()
                    })
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sidebar Rectangle Ads", fontSize = 13.sp)
                    Switch(checked = settings.monetizationEnabled, onCheckedChange = {
                        Toast.makeText(context, "Sidebar ads visible on large screens!", Toast.LENGTH_SHORT).show()
                    })
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Text("How it works: Once monetization and Google AdSense configurations are added under Site Settings, visits to published blog posts automatically trigger dynamic ledger payouts to your balance.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun PagesTabScreen(viewModel: BloggerViewModel) {
    val pagesList by viewModel.allPages.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Blog Static Pages",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Manage mandatory pages such as Contact, About, Terms.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            FloatingActionButton(
                onClick = { viewModel.isCreatingNewPage.value = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.testTag("new_page_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Page")
            }
        }

        if (pagesList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No static custom pages created yet.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pagesList) { page ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(page.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("/p/${page.slug}.html", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Row {
                                    IconButton(onClick = { viewModel.selectedPageForEdit.value = page }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Page", tint = MaterialTheme.colorScheme.secondary)
                                    }
                                    IconButton(onClick = { viewModel.deletePage(page.id) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Page", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(page.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeTabScreen(viewModel: BloggerViewModel, settings: SiteSettings) {
    var isDarkState by remember { mutableStateOf(settings.isDarkMode) }
    var selectedTheme by remember { mutableStateOf(settings.themeName) }
    
    val themes = listOf(
        "Coral Sunset" to Color(0xFFFC5F38),
        "Ocean Wave" to Color(0xFF007A87),
        "Forest Emerald" to Color(0xFF2E6B4E),
        "Midnight Charcoal" to Color(0xFF4A4E69)
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Theme Layout Customizer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Choose customizable color schemes and toggle Dark & Light mode.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // Active Theme Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Theme Live Preview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Column {
                        Text("Active Template Style: ${settings.themeName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Responsive mobile theme is fully parsed in dynamic colors.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // Available Themes Store Grid
        Text("Template Presets:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            themes.forEach { (name, color) ->
                val isSelected = selectedTheme == name
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedTheme = name
                            viewModel.updateTheme(name, isDarkState)
                        }
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).background(color, CircleShape))
                            Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Done, contentDescription = "Active Theme", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Mode switch control
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = "Dark Mode")
                    Text("Midnight Eye-Protect Dark Mode", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(
                    checked = isDarkState,
                    onCheckedChange = {
                        isDarkState = it
                        viewModel.updateTheme(selectedTheme, it)
                    },
                    modifier = Modifier.testTag("dark_mode_switch")
                )
            }
        }
    }
}

@Composable
fun SettingsTabScreen(viewModel: BloggerViewModel, settings: SiteSettings) {
    var title by remember { mutableStateOf(settings.blogTitle) }
    var desc by remember { mutableStateOf(settings.blogDescription) }
    var domain by remember { mutableStateOf(settings.customDomain) }
    var robots by remember { mutableStateOf(settings.robotsTxt) }
    var meta by remember { mutableStateOf(settings.metaDescription) }
    var monetized by remember { mutableStateOf(settings.monetizationEnabled) }
    var adCode by remember { mutableStateOf(settings.adSenseCode) }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Blogger Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Configure custom domains, crawlers, metadata, and scripts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Basic Configurations", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Blog Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Blog Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("SEO & Crawler Directives", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    
                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text("Custom Subdomain (e.g., world.blogger.com)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = meta,
                        onValueChange = { meta = it },
                        label = { Text("Meta Search Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    OutlinedTextField(
                        value = robots,
                        onValueChange = { robots = it },
                        label = { Text("Custom Robots.txt") },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        maxLines = 4
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Google AdSense Earnings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Switch(checked = monetized, onCheckedChange = { monetized = it })
                    }

                    if (monetized) {
                        OutlinedTextField(
                            value = adCode,
                            onValueChange = { adCode = it },
                            label = { Text("Google AdSense Publisher ID (e.g. ca-pub-xyz)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.saveSettings(
                        blogTitle = title,
                        blogDescription = desc,
                        customDomain = domain,
                        robotsTxt = robots,
                        metaDescription = meta,
                        monetizationEnabled = monetized,
                        adSenseCode = adCode
                    )
                    Toast.makeText(context, "Blogger Settings saved successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().testTag("save_settings_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Configurations", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AiToolkitScreen(viewModel: BloggerViewModel) {
    var articleTopic by remember { mutableStateOf("") }
    var articleKeywords by remember { mutableStateOf("") }
    var articleCategory by remember { mutableStateOf("Development") }

    var seoTitle by remember { mutableStateOf("") }
    var seoDesc by remember { mutableStateOf("") }

    var translationBody by remember { mutableStateOf("") }
    var targetLanguage by remember { mutableStateOf("Bangla") }

    val aiGenerating by viewModel.aiLoading.collectAsState()
    val aiDraftResult by viewModel.aiBlogPostIdeaResult.collectAsState()
    val aiSeoResult by viewModel.aiSeoSuggestionResult.collectAsState()
    val aiTransResult by viewModel.aiTranslateResult.collectAsState()

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Blogger AI Assistants",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Leverage Google Gemini 3.5 Flash to automatically draft articles, translate, and optimize SEO metadata.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Assistant Column A: Post Writer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("AI Blog Copywriter", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    
                    OutlinedTextField(
                        value = articleTopic,
                        onValueChange = { articleTopic = it },
                        label = { Text("What is your topic or headline?") },
                        placeholder = { Text("e.g. Jetpack Compose animation states") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = articleKeywords,
                        onValueChange = { articleKeywords = it },
                        label = { Text("Keywords to weave in naturally") },
                        placeholder = { Text("e.g. coroutine state, spring, transition") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (articleTopic.isNotEmpty()) {
                                viewModel.generateAIBlogPostDraft(articleTopic, articleCategory, articleKeywords)
                            } else {
                                Toast.makeText(context, "Give AI a topic first!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !aiGenerating,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (aiGenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("Draft Article via Gemini", color = Color.White)
                    }

                    if (aiDraftResult.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(aiDraftResult, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        viewModel.addPost(
                                            title = articleTopic,
                                            content = aiDraftResult,
                                            category = articleCategory,
                                            tags = articleKeywords,
                                            status = "Draft",
                                            thumbnailUrl = ""
                                        )
                                        viewModel.clearAIStates()
                                        Toast.makeText(context, "Draft Saved to Posts!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Add Draft to Posts", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Assistant Column B: SEO Generator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("AI Google SEO Tags Generator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    
                    OutlinedTextField(
                        value = seoTitle,
                        onValueChange = { seoTitle = it },
                        label = { Text("What is your draft title?") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = seoDesc,
                        onValueChange = { seoDesc = it },
                        label = { Text("Brief concept details") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (seoTitle.isNotEmpty()) {
                                viewModel.generateAIOptimizedSEO(seoTitle, seoDesc)
                            } else {
                                Toast.makeText(context, "Insert title to suggest tags!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !aiGenerating,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        if (aiGenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("Get Intelligent Keywords SEO", color = Color.White)
                    }

                    if (aiSeoResult.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(aiSeoResult, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Assistant Column C: Auto Translator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("AI Post Translator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                    
                    OutlinedTextField(
                        value = translationBody,
                        onValueChange = { translationBody = it },
                        label = { Text("Insert paragraph content to translate") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 5
                    )

                    OutlinedTextField(
                        value = targetLanguage,
                        onValueChange = { targetLanguage = it },
                        label = { Text("Target Language") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (translationBody.isNotEmpty()) {
                                viewModel.translateBlogPostAI(translationBody, targetLanguage)
                            } else {
                                Toast.makeText(context, "Paste article block first!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !aiGenerating,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        if (aiGenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("Translate content instantly", color = Color.White)
                    }

                    if (aiTransResult.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2E7D32).copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF2E7D32).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(aiTransResult, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// --- Dynamic Interactive Dialogs ---

@Composable
fun PostEditorDialog(
    post: BlogPost?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit,
    viewModel: BloggerViewModel
) {
    var title by remember { mutableStateOf(post?.title ?: "") }
    var content by remember { mutableStateOf(post?.content ?: "") }
    var category by remember { mutableStateOf(post?.category ?: "Development") }
    var tags by remember { mutableStateOf(post?.tags ?: "") }
    var status by remember { mutableStateOf(post?.status ?: "Published") }
    var coverUrl by remember { mutableStateOf(post?.thumbnailUrl ?: "") }

    val aiDraftText by viewModel.aiBlogPostIdeaResult.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    
    val context = LocalContext.current

    // Observe AI Text generation & pull into content field immediately
    LaunchedEffect(aiDraftText) {
        if (aiDraftText.isNotEmpty() && !aiDraftText.contains("Error") && !aiDraftText.contains("drafting")) {
            content = aiDraftText
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (post == null) "New Blogger Post" else "Edit Published Post", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    // Quick AI Writing trigger directly inside editor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Content Body", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Card(
                            modifier = Modifier.clickable(enabled = !aiLoading) {
                                if (title.isNotEmpty()) {
                                    viewModel.generateAIBlogPostDraft(title, category, tags)
                                } else {
                                    Toast.makeText(context, "Insert Title first to generate draft!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (aiLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Star, contentDescription = "AI Generate", modifier = Modifier.size(14.dp))
                                }
                                Text("AI Write Draft", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        maxLines = 10,
                        placeholder = { Text("Compose your story here or click 'AI Write Draft' to auto-generate markdown text.") }
                    )
                }

                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Tags (comma separated elements)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        label = { Text("Featured Image URL (Optional)") },
                        placeholder = { Text("e.g. https://domain.com/picture.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text("Publication State", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = status == "Published", onClick = { status = "Published" })
                            Text("Publish")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = status == "Draft", onClick = { status = "Draft" })
                            Text("Draft")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && content.isNotEmpty()) {
                        onSave(title, content, category, tags, status, coverUrl)
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Title and Content are mandatory!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("save_post_dialog_btn")
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PageEditorDialog(
    page: BlogPage?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(page?.title ?: "") }
    var content by remember { mutableStateOf(page?.content ?: "") }
    var slug by remember { mutableStateOf(page?.slug ?: "") }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (page == null) "New Static Page" else "Edit Static Page", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Page Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 8
                )

                OutlinedTextField(
                    value = slug,
                    onValueChange = { slug = it },
                    label = { Text("URL Slug (e.g. about-us)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && content.isNotEmpty() && slug.isNotEmpty()) {
                        onSave(title, content, slug)
                        onDismiss()
                    } else {
                        Toast.makeText(context, "All fields are mandatory!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("save_page_dialog_btn")
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
