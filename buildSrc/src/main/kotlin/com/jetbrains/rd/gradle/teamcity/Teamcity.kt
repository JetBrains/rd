package com.jetbrains.rd.gradle.teamcity

object Teamcity {
    private const val VERSION = "TEAMCITY_VERSION"

    fun onTeamcity() = System.getenv(VERSION) != null
}