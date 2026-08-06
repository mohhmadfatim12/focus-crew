package com.focuscrew.app.data.model

data class BlockProfile(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val packages: Set<String>,
    val isCustom: Boolean = false
)
