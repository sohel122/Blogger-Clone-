package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(entities = [BlogPost::class, BlogComment::class, BlogPage::class, SiteSettings::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bloggerDao(): BloggerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "blogger_clone_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class BloggerRepository(private val db: AppDatabase) {
    val dao: BloggerDao = db.bloggerDao()

    val allPosts: Flow<List<BlogPost>> = dao.getAllPostsFlow()
    val allComments: Flow<List<BlogComment>> = dao.getAllCommentsFlow()
    val allPages: Flow<List<BlogPage>> = dao.getAllPagesFlow()
    val settingsFlow: Flow<SiteSettings?> = dao.getSettingsFlow()

    fun getCommentsForPost(postId: Int): Flow<List<BlogComment>> = dao.getCommentsForPostFlow(postId)

    suspend fun getPostById(postId: Int): BlogPost? = withContext(Dispatchers.IO) {
        dao.getPostById(postId)
    }

    suspend fun savePost(post: BlogPost): Long = withContext(Dispatchers.IO) {
        dao.insertPost(post)
    }

    suspend fun deletePost(postId: Int) = withContext(Dispatchers.IO) {
        dao.deletePostById(postId)
    }

    suspend fun updatePost(post: BlogPost) = withContext(Dispatchers.IO) {
        dao.updatePost(post)
    }

    suspend fun saveComment(comment: BlogComment): Long = withContext(Dispatchers.IO) {
        dao.insertComment(comment)
    }

    suspend fun deleteComment(commentId: Int) = withContext(Dispatchers.IO) {
        dao.deleteCommentById(commentId)
    }

    suspend fun savePage(page: BlogPage): Long = withContext(Dispatchers.IO) {
        dao.insertPage(page)
    }

    suspend fun deletePage(pageId: Int) = withContext(Dispatchers.IO) {
        dao.deletePageById(pageId)
    }

    suspend fun updateSettings(settings: SiteSettings) = withContext(Dispatchers.IO) {
        dao.insertOrUpdateSettings(settings)
    }

    // Seed initial realistic Blogger blog context data
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val currentSettings = dao.getSettingsDirect()
        if (currentSettings == null) {
            // 1. Seed standard responsive custom settings
            dao.insertOrUpdateSettings(
                SiteSettings(
                    id = 1,
                    blogTitle = "My Tech Odyssey",
                    blogDescription = "A portal covering modern mobile paradigms, Jetpack Compose layouts, the future of AI, and sleek dynamic colors.",
                    customDomain = "mytechodyssey.blogger.com",
                    robotsTxt = "User-agent: *\nDisallow: /admin/\nSitemap: https://mytechodyssey.blogger.com/sitemap.xml",
                    metaDescription = "Explorer of Kotlin, AI-assisted development, Material Design 3, and elegant frontends.",
                    themeName = "Coral Sunset",
                    isDarkMode = false,
                    monetizationEnabled = true,
                    adSenseCode = "ca-pub-6428482542566129"
                )
            )

            // 2. Seed standard professional blogging posts
            val post1Id = dao.insertPost(
                BlogPost(
                    title = "Building Fluid Android Frontends in 2026",
                    category = "Development",
                    tags = "Android, Compose, M3",
                    views = 1240,
                    thumbnailUrl = "https://images.unsplash.com/photo-1607799279861-4dd421887fb3?auto=format&fit=crop&w=600&q=80",
                    status = "Published",
                    publishTime = System.currentTimeMillis() - 86400000 * 3, // 3 days ago
                    content = """Jetpack Compose has completely revolutionized how we approach mobile design on the Android platform. By replacing rigid XML layouts with reactive, state-driven Kotlin components, we can construct interfaces that respond fluidly to animations, state flows, and dark thematic changes.

Key Pillars of Dynamic Layouts in 2026:
1. Material Design 3 (M3): Harnessing generative primary tones that match user wallpaper choices perfectly.
2. Window Size Classes: Delivering adaptive sidebar hierarchies on larger tablet viewports, contrasting with classic mobile tab navigations.
3. Edge-to-Edge Fluidity: Writing zero-padding root containers and using window inserts manually so content fits naturally behind state bars.

In this series, we will examine how real-time Room state updates drive reactive lazy columns with almost zero layout stutter."""
                )
            )

            val post2Id = dao.insertPost(
                BlogPost(
                    title = "The Evolution of Assistant Paradigms and Gemini 3.5",
                    category = "Artificial Intelligence",
                    tags = "AI, Gemini, LLM",
                    views = 8520,
                    thumbnailUrl = "https://images.unsplash.com/photo-1677442136019-21780efad99a?auto=format&fit=crop&w=600&q=80",
                    status = "Published",
                    publishTime = System.currentTimeMillis() - 86400000 * 1, // 1 day ago
                    content = """Generative AI models are no longer standalone text predictors. With models like Gemini 3.5 Flash, the speed of developer pipelines is enhanced through structured JSON modes, integrated system prompts, and multi-modal contexts.

As mobile engineers, incorporating contextual summarization or auto-translation into blog editors becomes a trivial five-minute REST implementation rather than requiring large remote cloud clusters. 

This post demonstrates how we can design and integrate local prompts directly using secure environment keys to generate rich blogging descriptions safely."""
                )
            )

            val post3Id = dao.insertPost(
                BlogPost(
                    title = "Premium Layout Secrets: Finding the Perfect Palette",
                    category = "UI/UX Design",
                    tags = "Design, Color, Material3",
                    views = 310,
                    thumbnailUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=600&q=80",
                    status = "Draft",
                    publishTime = System.currentTimeMillis() + 3600000 * 5, // Scheduled/Draft
                    content = """Choosing colors isn't simply picking hex numbers. The most elegant applications maintain visual balance by treating negative space with equal respect.

When designing themes, we can pair visual weights (such as a 60% neutral paper palette, 30% structural gray containers, and 10% high-contrast coral accents) to command attention exactly where the user expects to interact. Let's design a perfect dark slate scheme together!"""
                )
            )

            // 3. Seed starter comments
            dao.insertComment(
                BlogComment(
                    postId = post1Id.toInt(),
                    author = "Riyad Khan",
                    content = "This is wonderful! The state-driven approach in Compose really does make list interactions feel extremely responsive.",
                    timestamp = System.currentTimeMillis() - 86400000 * 2,
                    isApproved = true
                )
            )
            dao.insertComment(
                BlogComment(
                    postId = post1Id.toInt(),
                    author = "Labone",
                    content = "Could you write a follow-up post detailing how to manage animations inside adaptive layouts?",
                    timestamp = System.currentTimeMillis() - 86400000 * 1,
                    isApproved = true
                )
            )
            dao.insertComment(
                BlogComment(
                    postId = post2Id.toInt(),
                    author = "Muntasir",
                    content = "Fascinating! Integrating Gemini directly via REST makes it light and highly customizable.",
                    timestamp = System.currentTimeMillis() - 3600000 * 4,
                    isApproved = true
                )
            )

            // 4. Seed key generic pages
            dao.insertPage(
                BlogPage(
                    title = "About This Blog",
                    slug = "about",
                    content = "This is a concept blog designed to illustrate the raw capabilities of Blogger Clone in Android. Powered by Kotlin, Room Database, Jetpack Compose, and Google Gemini API.",
                    timestamp = System.currentTimeMillis() - 86400000 * 5
                )
            )
            dao.insertPage(
                BlogPage(
                    title = "Contact Us",
                    slug = "contact",
                    content = "Got inquiries? Reach out to support at labone9500@gmail.com for feedback. We welcome suggestions on blogging layout extensions and templates.",
                    timestamp = System.currentTimeMillis() - 86400000 * 4
                )
            )
            dao.insertPage(
                BlogPage(
                    title = "Privacy Policy",
                    slug = "privacy",
                    content = "At Blogger Clone, your privacy is highly respected. All drafts, postings, settings, and comment configurations are saved securely inside your offline-first local device database.",
                    timestamp = System.currentTimeMillis() - 86400000 * 3
                )
            )
        }
    }
}
