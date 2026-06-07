package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "blog_posts")
data class BlogPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val status: String = "Published", // "Published", "Draft", "Scheduled"
    val publishTime: Long = System.currentTimeMillis(),
    val category: String = "General",
    val tags: String = "",
    val views: Int = 0,
    val thumbnailUrl: String = ""
)

@Entity(tableName = "blog_comments")
data class BlogComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val author: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isApproved: Boolean = true
)

@Entity(tableName = "blog_pages")
data class BlogPage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val slug: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "site_settings")
data class SiteSettings(
    @PrimaryKey val id: Int = 1,
    val blogTitle: String = "My Blogger Clone",
    val blogDescription: String = "A space to share my tech explorations, layout designs, and creative write-ups.",
    val customDomain: String = "",
    val robotsTxt: String = "User-agent: *\nDisallow: /private/",
    val metaDescription: String = "Blogger Clone site sharing amazing stories.",
    val themeName: String = "Coral Sunset", // "Coral Sunset", "Ocean Wave", "Forest Emerald", "Midnight Charcoal"
    val isDarkMode: Boolean = false,
    val monetizationEnabled: Boolean = false,
    val adSenseCode: String = ""
)

// --- Data Access Objects ---

@Dao
interface BloggerDao {
    // Posts
    @Query("SELECT * FROM blog_posts ORDER BY publishTime DESC")
    fun getAllPostsFlow(): Flow<List<BlogPost>>

    @Query("SELECT * FROM blog_posts WHERE id = :id")
    suspend fun getPostById(id: Int): BlogPost?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: BlogPost): Long

    @Update
    suspend fun updatePost(post: BlogPost)

    @Query("DELETE FROM blog_posts WHERE id = :id")
    suspend fun deletePostById(id: Int)

    // Comments
    @Query("SELECT * FROM blog_comments ORDER BY timestamp DESC")
    fun getAllCommentsFlow(): Flow<List<BlogComment>>

    @Query("SELECT * FROM blog_comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPostFlow(postId: Int): Flow<List<BlogComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: BlogComment): Long

    @Query("DELETE FROM blog_comments WHERE id = :id")
    suspend fun deleteCommentById(id: Int)

    // Pages
    @Query("SELECT * FROM blog_pages ORDER BY timestamp DESC")
    fun getAllPagesFlow(): Flow<List<BlogPage>>

    @Query("SELECT * FROM blog_pages WHERE id = :id")
    suspend fun getPageById(id: Int): BlogPage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: BlogPage): Long

    @Query("DELETE FROM blog_pages WHERE id = :id")
    suspend fun deletePageById(id: Int)

    // Settings (Exactly 1 record)
    @Query("SELECT * FROM site_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<SiteSettings?>

    @Query("SELECT * FROM site_settings WHERE id = 1")
    suspend fun getSettingsDirect(): SiteSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SiteSettings)
}
