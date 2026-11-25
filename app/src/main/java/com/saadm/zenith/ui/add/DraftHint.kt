package com.saadm.zenith.ui.add

private const val ALWAYS_FIRST = 5
private const val THEN_EVERY = 20

/** The "closing keeps a draft" reminder: the first few times the sheet opens, then only now and then. */
fun showDraftHint(timesOpened: Int): Boolean = timesOpened < ALWAYS_FIRST || timesOpened % THEN_EVERY == 0
