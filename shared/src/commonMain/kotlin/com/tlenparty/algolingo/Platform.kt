package com.tlenparty.algolingo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform