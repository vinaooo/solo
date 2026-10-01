package io.github.vinaooo.solo.buildlogic

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class AppVersionTest {

    @Test
    fun `a tagged commit takes the tag as its name and the commit count as its code`() {
        AppVersion.fromGit(describe = "v1.2.3", commitCount = "42", shallow = false) shouldBe AppVersion(42, "1.2.3")
    }

    @Test
    fun `a commit after a tag says how far past the tag it is`() {
        AppVersion.fromGit(describe = "v1.2.3-4-gabc1234", commitCount = "46", shallow = false).name shouldBe
            "1.2.3-4-gabc1234"
    }

    @Test
    fun `before the first tag the name is 0_0_0 with the commit hash`() {
        AppVersion.fromGit(describe = "abc1234", commitCount = "26", shallow = false) shouldBe
            AppVersion(26, "0.0.0-gabc1234")
    }

    @Test
    fun `surrounding whitespace from git output is ignored`() {
        AppVersion.fromGit(describe = "v1.0.0\n", commitCount = " 7\n", shallow = false) shouldBe AppVersion(7, "1.0.0")
    }

    @Test
    fun `outside a git checkout the build still works with a placeholder version`() {
        AppVersion.fromGit(describe = null, commitCount = null, shallow = false) shouldBe AppVersion(1, "0.0.0-unknown")
    }

    @Test
    fun `a shallow clone fails, because its commit count is too low for Play`() {
        shouldThrow<IllegalStateException> {
            AppVersion.fromGit(describe = "v1.0.0", commitCount = "1", shallow = true)
        }.message shouldContain "fetch-depth: 0"
    }

    @Test
    fun `a release tag that is not vMAJOR_MINOR_PATCH fails and names the tag`() {
        shouldThrow<IllegalStateException> {
            AppVersion.fromGit(describe = "v1.0-2-gabc1234", commitCount = "9", shallow = false)
        }.message shouldContain "v1.0"
    }

    @Test
    fun `a commit count that is not a positive number fails`() {
        shouldThrow<IllegalStateException> {
            AppVersion.fromGit(describe = "v1.0.0", commitCount = "0", shallow = false)
        }
    }
}
