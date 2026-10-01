package io.github.vinaooo.solo.buildlogic

/**
 * The app's version, read from git: [code] is the number of commits up to HEAD, so it grows with every merge into
 * master, and [name] comes from the latest `vMAJOR.MINOR.PATCH` tag.
 */
data class AppVersion(val code: Int, val name: String) {
    companion object {
        /** Arguments for `git describe`; `--always` falls back to the short hash before the first release tag. */
        val describeArguments = listOf("describe", "--tags", "--match", "v[0-9]*", "--always")

        private val releaseTag = Regex("""v(\d+\.\d+\.\d+)(-\d+-g[0-9a-f]+)?""")
        private val unknown = AppVersion(1, "0.0.0-unknown")

        /**
         * Builds the version from the output of `git describe` ([describeArguments]) and `git rev-list --count HEAD`,
         * both null outside a git checkout. A shallow clone fails, because its commit count would be lower than the
         * last upload's and Play would reject the build.
         */
        fun fromGit(describe: String?, commitCount: String?, shallow: Boolean): AppVersion {
            if (describe == null || commitCount == null) return unknown
            check(!shallow) {
                "Shallow git clone: the commit count can't be the version code. Fetch the full history " +
                    "(actions/checkout with fetch-depth: 0)."
            }
            val code = commitCount.trim().toIntOrNull()
            check(code != null && code > 0) { "git rev-list --count gave \"${commitCount.trim()}\", not a commit count." }
            val description = describe.trim()
            return AppVersion(code, nameFrom(description))
        }

        private fun nameFrom(description: String): String {
            if (!description.startsWith("v")) return "0.0.0-g$description"
            val match = checkNotNull(releaseTag.matchEntire(description)) {
                "Release tag in \"$description\" is not vMAJOR.MINOR.PATCH, for example v1.0.0."
            }
            return match.groupValues[1] + match.groupValues[2]
        }
    }
}
