package com.recentr.core.data

import com.recentr.core.model.Rule

/**
 * Minutes per package used since the Start of each limit running today ([Rule.activeSince]): what
 * a limit trips on, and its "used / budget". Only the starts of running limits are read.
 */
data class LimitUsage(val sinceStart: Map<Long, Map<String, Int>>) {

    /** Minutes [rule]'s apps were used since its Start, or null when it isn't started or wasn't read. */
    fun of(rule: Rule): Int? = rule.activeSince?.let(sinceStart::get)?.let(rule::usedOf)
}
