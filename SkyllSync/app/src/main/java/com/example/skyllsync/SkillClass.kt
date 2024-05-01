package com.example.skyllsync
class SkillClass {
    enum class SkillProgression(val level: Int, val description: String, val imagePath: String) {
        LVL0(0, "No Way", "progress0"),
        LVL1(1, "With Active Spot", "progress1"),
        LVL2(2, "With Light Spot", "progress2"),
        LVL3(3, "No spot but not yet able", "progress3"),
        LVL4(4, "Clumsy but correct", "progress4"),
        LVL5(5, "Mastered", "progress5")
    }


    enum class SkillPriority(val level: Int, val color: String, val imagePath: String) {
        LVL0(0, "#E0E0E0", "priority_0"),
        LVL1(1, "#D0FF00", "priority_1"),
        LVL2(2, "#FFFF40", "priority_2"),
        LVL3(3, "#FFCA60", "priority_3"),
        LVL4(4, "#FF6040", "priority_4"),
        LVL5(5, "#E80000", "priority_5"),
        FRIENDREQUEST(6, "#6060FF", "friend_request")
    }

    enum class pairUpStatus(val status: Int, val description: String, val action : String){
        NOT_PAIRED(0, "Not Paired", "PAIR UP !"),
        PENDING_RECEIVED(1, "REQUEST RECEIVED, PENDING", "RESPOND TO REQUEST"),
        PENDING_SENT(2, "REQUEST SENT, PENDING", "REQUEST SENT, PENDING"),
        PAIRED(3, "Paired", "UNPAIR")
    }

    companion object {
        fun getProgressionPathFromNumber(number: Int): String {
            val skillProgressionElement = SkillProgression.values().find {
                it.level == number
            }
            return skillProgressionElement?.imagePath ?: "progress0"
        }

        fun getProgressionNameFromNumber(number: Int): String {
            val skillProgressionElement = SkillProgression.values().find {
                it.level == number
            }
            return skillProgressionElement?.description ?: "No Way"
        }

        fun getPriorityColorFromNumber(number: Int): String {
            val skillPriorityElement = SkillPriority.values().find {
                it.level == number
            }
            return skillPriorityElement?.color ?: "#808080"
        }

        fun getPriorityPathFromNumber(number: Int): String {
            val skillPriorityElement = SkillPriority.values().find {
                it.level == number
            }
            return skillPriorityElement?.imagePath ?: "priority_0"
        }

        fun getPairUpStatusFromNumber(number: Int): String {
            val pairUpStatusElement = pairUpStatus.values().find {
                it.status == number
            }
            return pairUpStatusElement?.description ?: "Not Paired"
        }

        fun getPairUpActionFromNumber(number: Int): String {
            val pairUpStatusElement = pairUpStatus.values().find {
                it.status == number
            }
            return pairUpStatusElement?.action ?: "PAIR UP !"
        }

    }


}