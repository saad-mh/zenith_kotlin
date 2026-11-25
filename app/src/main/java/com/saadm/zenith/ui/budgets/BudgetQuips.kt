package com.saadm.zenith.ui.budgets

import com.saadm.zenith.domain.budget.BudgetStatus
import kotlin.random.Random

private val onTrack = listOf(
    "Looking good, money boss",
    "Budget? Never heard of her",
    "You're basically a financial influencer",
    "This is the way",
    "Certified responsible adult moment",
    "The spreadsheet gods are pleased",
    "Still got that bag",
    "Living within your means... legendary",
    "Keep cooking (the good kind)",
    "Financially unbothered",
    "Main character energy with money",
    "You're doing better than most of us",
    "Budget status: thriving",
    "No notes, king/queen",
    "This is peak adulthood"
)

private val nearingLimit = listOf(
    "Slow down champ",
    "Almost time to lock in",
    "The end is nigh... kind of",
    "You're playing with fire",
    "Budget is side-eyeing you",
    "Maybe skip the next coffee?",
    "Approaching the danger zone",
    "It's getting spicy in here",
    "One more purchase and it's over",
    "The vibe is shifting...",
    "You're on thin ice, money-wise",
    "Time to start sweating a little",
    "Budget is whispering 'don't do it'",
    "This is your final warning (cute version)",
    "Almost cooked"
)

private val overBudget = listOf(
    "You're cooked",
    "It's so over",
    "Financially deceased",
    "The budget has left the chat",
    "We lost him/her",
    "Absolute cinema (of poor decisions)",
    "Budget status: deceased",
    "This is not a drill",
    "You really went for it huh",
    "Certified financial terrorist",
    "The bag is gone",
    "Skill issue detected",
    "This is why we can't have nice things",
    "Budget said 'I'm out'",
    "You've been financially unalive'd"
)

// {x} is the amount still left.
private val remaining = listOf(
    "{x} away from devastation",
    "{x} between you and financial ruin",
    "Only {x} left before chaos",
    "{x} standing between you and the void",
    "You've got {x} of runway left",
    "{x} until the budget ghosts you",
    "Still {x} from the point of no return"
)

// {x} is the amount over the limit.
private val overBy = listOf(
    "Over by {x}... impressive",
    "You went {x} past the line",
    "{x} into the danger zone",
    "Currently {x} deep in the red",
    "You're {x} over and thriving (not really)"
)

/** Picked once per launch, so the lines change between launches but not while scrolling. */
val sessionQuipSalt: Int = Random.nextInt(0, 10_000)

fun quipsFor(status: BudgetStatus): List<String> = when (status) {
    BudgetStatus.ON_TRACK -> onTrack
    BudgetStatus.NEARING -> nearingLimit + remaining
    BudgetStatus.OVER -> overBudget + overBy
}

/** The line for [key], with [amount] (left or over) filled in. */
fun budgetQuip(status: BudgetStatus, key: Int, amount: String): String {
    val lines = quipsFor(status)
    return lines[Math.floorMod(key, lines.size)].replace("{x}", amount)
}
