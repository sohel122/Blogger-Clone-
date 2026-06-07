package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

class BloggerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = BloggerRepository(db)

    // Flow observations from Room
    val allPosts: StateFlow<List<BlogPost>> = repository.allPosts
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allComments: StateFlow<List<BlogComment>> = repository.allComments
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allPages: StateFlow<List<BlogPage>> = repository.allPages
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val settings: StateFlow<SiteSettings> = repository.settingsFlow
        .map { it ?: SiteSettings() }
        .stateIn(viewModelScope, SharingStarted.Lazily, SiteSettings())

    // UI Navigation State
    val activeTab = MutableStateFlow("Posts") // "Dashboard", "Posts", "Comments", "Earnings", "Pages", "Theme", "Settings", "AI Toolkit"

    // Form inputs & Editing references
    val selectedPostForEdit = MutableStateFlow<BlogPost?>(null)
    val selectedPageForEdit = MutableStateFlow<BlogPage?>(null)
    val isCreatingNewPost = MutableStateFlow(false)
    val isCreatingNewPage = MutableStateFlow(false)

    // Simulated analytical state
    val simulatedLiveVisitors = MutableStateFlow(12)
    val simulatedDailyHits = MutableStateFlow(1420)
    val simulatedMockBalance = MutableStateFlow(182.45)
    val isSimulatingState = MutableStateFlow(false)

    // AI Generation states
    val aiLoading = MutableStateFlow(false)
    val aiBlogPostIdeaResult = MutableStateFlow("")
    val aiSeoSuggestionResult = MutableStateFlow("")
    val aiTranslateResult = MutableStateFlow("")

    init {
        // Automatically seed database if empty on startup
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            simulatePeriodicVisitors()
        }
    }

    private fun simulatePeriodicVisitors() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(45000) // Change visitor count occasionally
                val delta = Random.nextInt(-3, 4)
                val current = simulatedLiveVisitors.value
                simulatedLiveVisitors.value = (current + delta).coerceAtLeast(3)
            }
        }
    }

    fun selectTab(tab: String) {
        activeTab.value = tab
    }

    // --- Post operations ---
    fun addPost(title: String, content: String, category: String, tags: String, status: String, thumbnailUrl: String) {
        viewModelScope.launch {
            val thumbnail = if (thumbnailUrl.isNotEmpty()) thumbnailUrl else {
                // Return a nice landscape illustration category placeholder
                when (category.lowercase()) {
                    "ui/ux design", "design" -> "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=600&q=80"
                    "development", "tech" -> "https://images.unsplash.com/photo-1607799279861-4dd421887fb3?auto=format&fit=crop&w=600&q=80"
                    "artificial intelligence", "ai" -> "https://images.unsplash.com/photo-1677442136019-21780efad99a?auto=format&fit=crop&w=600&q=80"
                    else -> "https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&fit=crop&w=600&q=80"
                }
            }

            val newPost = BlogPost(
                title = title,
                content = content,
                category = category,
                tags = tags,
                status = status,
                thumbnailUrl = thumbnail,
                publishTime = System.currentTimeMillis()
            )
            repository.savePost(newPost)
            isCreatingNewPost.value = false
            simulateAdSenseEarning(0.45) // small revenue increment
        }
    }

    fun updatePost(id: Int, title: String, content: String, category: String, tags: String, status: String, thumbnailUrl: String, views: Int) {
        viewModelScope.launch {
            val updatedPost = BlogPost(
                id = id,
                title = title,
                content = content,
                category = category,
                tags = tags,
                status = status,
                thumbnailUrl = thumbnailUrl,
                publishTime = System.currentTimeMillis(),
                views = views
            )
            repository.updatePost(updatedPost)
            selectedPostForEdit.value = null
        }
    }

    fun deletePost(id: Int) {
        viewModelScope.launch {
            repository.deletePost(id)
        }
    }

    // --- Page operations ---
    fun addPage(title: String, content: String, slug: String) {
        viewModelScope.launch {
            val newPage = BlogPage(
                title = title,
                content = content,
                slug = slug.lowercase().replace(" ", "-"),
                timestamp = System.currentTimeMillis()
            )
            repository.savePage(newPage)
            isCreatingNewPage.value = false
        }
    }

    fun updatePage(id: Int, title: String, content: String, slug: String, timestamp: Long) {
        viewModelScope.launch {
            val updatedPage = BlogPage(
                id = id,
                title = title,
                content = content,
                slug = slug.lowercase().replace(" ", "-"),
                timestamp = timestamp
            )
            repository.savePage(updatedPage)
            selectedPageForEdit.value = null
        }
    }

    fun deletePage(id: Int) {
        viewModelScope.launch {
            repository.deletePage(id)
        }
    }

    // --- Comment operations ---
    fun addComment(postId: Int, author: String, content: String) {
        viewModelScope.launch {
            val newComment = BlogComment(
                postId = postId,
                author = author,
                content = content,
                timestamp = System.currentTimeMillis(),
                isApproved = true
            )
            repository.saveComment(newComment)
            
            // Increment statistics views of that post
            val post = repository.getPostById(postId)
            if (post != null) {
                repository.updatePost(post.copy(views = post.views + 15))
            }
        }
    }

    fun deleteComment(id: Int) {
        viewModelScope.launch {
            repository.deleteComment(id)
        }
    }

    fun getCommentsForPost(postId: Int): Flow<List<BlogComment>> {
        return repository.getCommentsForPost(postId)
    }

    // --- Settings / Design Theme operations ---
    fun saveSettings(blogTitle: String, blogDescription: String, customDomain: String, robotsTxt: String, metaDescription: String, monetizationEnabled: Boolean, adSenseCode: String) {
        viewModelScope.launch {
            val current = settings.value
            val updated = current.copy(
                blogTitle = blogTitle,
                blogDescription = blogDescription,
                customDomain = customDomain,
                robotsTxt = robotsTxt,
                metaDescription = metaDescription,
                monetizationEnabled = monetizationEnabled,
                adSenseCode = adSenseCode
            )
            repository.updateSettings(updated)
        }
    }

    fun updateTheme(themeName: String, isDarkMode: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            val updated = current.copy(
                themeName = themeName,
                isDarkMode = isDarkMode
            )
            repository.updateSettings(updated)
        }
    }

    // --- Analytical Traffic Simulation ---
    fun refreshLiveAnalytics() {
        viewModelScope.launch {
            isSimulatingState.value = true
            kotlinx.coroutines.delay(1200) // add nice interactive feel
            
            // Generate some random engagement metrics
            simulatedLiveVisitors.value = Random.nextInt(15, 68)
            val addedDailyHits = Random.nextInt(180, 520)
            simulatedDailyHits.value += addedDailyHits
            
            // Small financial balance mock calculation
            if (settings.value.monetizationEnabled) {
                val addedBalance = addedDailyHits * Random.nextDouble(0.002, 0.008)
                simulatedMockBalance.value = (simulatedMockBalance.value + addedBalance)
            }
            
            // Update individual post views elements too!
            val posts = allPosts.value
            posts.forEach { post ->
                if (post.status == "Published") {
                    val viewsAdded = Random.nextInt(25, 140)
                    repository.updatePost(post.copy(views = post.views + viewsAdded))
                }
            }

            isSimulatingState.value = false
        }
    }

    private fun simulateAdSenseEarning(amount: Double) {
        if (settings.value.monetizationEnabled) {
            simulatedMockBalance.value += amount
        }
    }

    // --- Gemini AI Actions ---
    fun generateAIBlogPostDraft(title: String, category: String, keywords: String) {
        viewModelScope.launch {
            aiLoading.value = true
            aiBlogPostIdeaResult.value = "AI is drafting your creative content, please wait..."
            try {
                val result = GeminiHelper.generateBlogPostDraft(title, category, keywords)
                aiBlogPostIdeaResult.value = result
            } catch (e: Exception) {
                aiBlogPostIdeaResult.value = "Failed to draft blog post: ${e.localizedMessage}"
            } finally {
                aiLoading.value = false
            }
        }
    }

    fun generateAIOptimizedSEO(title: String, description: String) {
        viewModelScope.launch {
            aiLoading.value = true
            aiSeoSuggestionResult.value = "Analyzing meta keywords, please wait..."
            try {
                val result = GeminiHelper.generateSeoTagSuggestions(title, description)
                aiSeoSuggestionResult.value = result
            } catch (e: Exception) {
                aiSeoSuggestionResult.value = "Failed to suggest SEO structures: ${e.localizedMessage}"
            } finally {
                aiLoading.value = false
            }
        }
    }

    fun translateBlogPostAI(content: String, targetLang: String) {
        viewModelScope.launch {
            aiLoading.value = true
            aiTranslateResult.value = "Translating content bodies, please wait..."
            try {
                val result = GeminiHelper.generateTranslation(content, targetLang)
                aiTranslateResult.value = result
            } catch (e: Exception) {
                aiTranslateResult.value = "Failed to translate: ${e.localizedMessage}"
            } finally {
                aiLoading.value = false
            }
        }
    }

    fun clearAIStates() {
        aiBlogPostIdeaResult.value = ""
        aiSeoSuggestionResult.value = ""
        aiTranslateResult.value = ""
    }
}
