@file:Suppress("MemberVisibilityCanBePrivate")

package com.shark.svgaplayer_base.network

import okhttp3.Response

class HttpException(val response: Response) : RuntimeException("HTTP ${response.code}: ${response.message}")
