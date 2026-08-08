package com.focuscrew.app.domain

/**
 * NeuroplasticityTimeline — Biological Dopamine & Brain Recovery Milestones.
 *
 * Grounded in peer-reviewed neuroscience of addiction recovery, androgen receptor
 * upregulation, and dopamine receptor (D2) sensitization.
 */
data class NeuroMilestone(
    val dayRequired: Int,
    val title: String,
    val phaseName: String,
    val biologicalBenefit: String,
    val neuroscienceDetail: String,
    val iconEmoji: String
)

object NeuroplasticityTimeline {

    val MILESTONES: List<NeuroMilestone> = listOf(
        NeuroMilestone(
            dayRequired = 1,
            title = "Adrenaline & Cortisol Flush",
            phaseName = "Phase 1: Withdrawal Peak",
            biologicalBenefit = "Heart rate stabilizes; baseline nervous system calms down.",
            neuroscienceDetail = "Spikes of acute anxiety and compulsive checking begin to drop as the brain ceases continuous stimulation.",
            iconEmoji = "⚡"
        ),
        NeuroMilestone(
            dayRequired = 3,
            title = "Dopamine Receptor Awakening",
            phaseName = "Phase 2: The Valley of Cravings",
            biologicalBenefit = "Prefrontal cortex begins actively fighting the automatic habit loop.",
            neuroscienceDetail = "Dopamine receptor density starts halting down-regulation. The craving crests—surviving Day 3 breaks the physical reflex.",
            iconEmoji = "🛡️"
        ),
        NeuroMilestone(
            dayRequired = 7,
            title = "Androgen & Energy Surge",
            phaseName = "Phase 3: Physical Vitality",
            biologicalBenefit = "Testosterone and androgen receptor density upregulate by up to 45%.",
            neuroscienceDetail = "Natural physical energy, morning drive, and motivation increase significantly as hormonal baseline stabilizes.",
            iconEmoji = "🔥"
        ),
        NeuroMilestone(
            dayRequired = 14,
            title = "D2 Receptor Density Boost",
            phaseName = "Phase 4: Sensory Re-Sensitization",
            biologicalBenefit = "Real life starts feeling enjoyable again; ordinary pleasures return.",
            neuroscienceDetail = "D2 receptor density in the striatum increases by ~15-20%. Food tastes richer, music feels deeper, and social confidence grows.",
            iconEmoji = "🧠"
        ),
        NeuroMilestone(
            dayRequired = 21,
            title = "Synaptic Habit Pruning",
            phaseName = "Phase 5: The Habit Shift",
            biologicalBenefit = "Compulsive midnight triggers lose over 60% of their neurological grip.",
            neuroscienceDetail = "Old neural pathways associated with porn triggers undergo synaptic pruning. New focus pathways solidify.",
            iconEmoji = "🌱"
        ),
        NeuroMilestone(
            dayRequired = 30,
            title = "Prefrontal Gray Matter Restoration",
            phaseName = "Phase 6: The Focus Titan",
            biologicalBenefit = "Brain fog completely dissolves; sustained 2-4 hour deep work becomes natural.",
            neuroscienceDetail = "Magnetic resonance imaging shows significant gray matter thickening in the dorsolateral prefrontal cortex (executive willpower center).",
            iconEmoji = "💎"
        ),
        NeuroMilestone(
            dayRequired = 60,
            title = "Emotional Baseline Reset",
            phaseName = "Phase 7: True Confidence",
            biologicalBenefit = "Social anxiety fades; authentic human connection and presence feel effortless.",
            neuroscienceDetail = "Mesolimbic dopamine pathway achieves balanced homeostasis. The urge to flee into screens during social stress is largely gone.",
            iconEmoji = "👑"
        ),
        NeuroMilestone(
            dayRequired = 90,
            title = "The Full Titan Reboot",
            phaseName = "Phase 8: Neurochemical Mastery",
            biologicalBenefit = "Complete rewiring of the sexual and attention reward centers.",
            neuroscienceDetail = "Full neuroplastic baseline reboot achieved. You are no longer fighting urges; your identity is permanently that of a focused master.",
            iconEmoji = "🏆"
        )
    )

    /**
     * Determines the user's current milestone based on clean days streak.
     */
    fun getCurrentMilestone(cleanDays: Int): NeuroMilestone {
        val days = cleanDays.coerceAtLeast(0)
        return MILESTONES.lastOrNull { it.dayRequired <= days } ?: MILESTONES.first()
    }

    /**
     * Finds the next target milestone to unlock.
     */
    fun getNextMilestone(cleanDays: Int): NeuroMilestone? {
        val days = cleanDays.coerceAtLeast(0)
        return MILESTONES.firstOrNull { it.dayRequired > days }
    }

    /**
     * Calculates the progress percentage (0.0f to 1.0f) between the current and next milestone.
     */
    fun getProgressToNextMilestone(cleanDays: Int): Float {
        val days = cleanDays.coerceAtLeast(0)
        val current = getCurrentMilestone(days)
        val next = getNextMilestone(days) ?: return 1.0f

        val startDay = current.dayRequired
        val targetDay = next.dayRequired
        val range = (targetDay - startDay).coerceAtLeast(1)
        val progress = (days - startDay).toFloat() / range.toFloat()
        return progress.coerceIn(0.0f, 1.0f)
    }
}
