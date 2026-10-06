package io.github.vinaooo.solo.domain.model

/**
 * How kind the deals are. Easy and Normal deal only games a solver has won (see `WinnableDeals`): Easy the ones it won
 * with little search, Normal any. Hard deals any shuffle, winnable or not.
 */
enum class Difficulty { EASY, NORMAL, HARD }
