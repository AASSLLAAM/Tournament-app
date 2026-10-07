package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.Tournament
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EsportsRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun testSeedingAndObservingTournaments() = runBlocking {
    signInTestUser("gamer1@arenawar.in")
    val repo = EsportsRepository(firestore)

    repo.seedDefaultTournamentsIfEmpty()

    val tournaments = withTimeout(5000L) {
      repo.observeTournaments().first { it.isNotEmpty() }
    }

    assertTrue(tournaments.size >= 3)
    val ffTournament = tournaments.find { it.gameCategoryName == "FREE_FIRE" }
    assertNotNull(ffTournament)
  }

  @Test
  fun testUserWalletAndJoiningTournamentDeduction() = runBlocking {
    val uid = signInTestUser("player10@arenawar.in")
    val repo = EsportsRepository(firestore)

    val profile = repo.ensureUserProfileExists("player10@arenawar.in", "PlayerTen")
    assertEquals(500, profile.walletBalance)

    repo.seedDefaultTournamentsIfEmpty()
    val tournaments = withTimeout(5000L) {
      repo.observeTournaments().first { it.isNotEmpty() }
    }
    val targetTour = tournaments.first { it.entryFee == 50 }

    val joinResult = repo.joinTournament(targetTour)
    assertTrue(joinResult.isSuccess)

    val userSnap = firestore.collection("users").document(uid).get().await()
    val updatedBalance = (userSnap.getLong("walletBalance") ?: 0L).toInt()
    assertEquals(450, updatedBalance)
  }

  @Test
  fun testAdminCanCreateTournamentAndDeclareWinner() = runBlocking {
    signInTestUser("aslamhussain096@gmail.com")
    val repo = EsportsRepository(firestore)

    val adminProfile = repo.ensureUserProfileExists("aslamhussain096@gmail.com", "MainAdmin")
    assertTrue(adminProfile.isAdmin)

    val createResult = repo.createTournament(
      title = "BGMI Admin Invitational",
      gameCategory = "BGMI",
      map = "Erangel",
      format = "Squad",
      scheduleDate = "Today",
      scheduleTime = "11:00 PM",
      entryFee = 150,
      prizePool = 10000,
      perKill = 30,
      totalSlots = 100
    )
    assertTrue(createResult.isSuccess)
    val tourId = createResult.getOrThrow()

    val declareResult = repo.declareWinner(tourId, "Team GodLike")
    assertTrue(declareResult.isSuccess)

    val tourSnap = firestore.collection("tournaments").document(tourId).get().await()
    assertEquals("Team GodLike", tourSnap.getString("winnerName"))
    assertEquals("Completed", tourSnap.getString("statusText"))
  }

  @Test
  fun testRazorpayAddMoneyUpdatesWalletAndCreatesTransaction() = runBlocking {
    val uid = signInTestUser("razorpay_user@arenawar.in")
    val repo = EsportsRepository(firestore)

    val profile = repo.ensureUserProfileExists("razorpay_user@arenawar.in", "RazorPayUser")
    assertEquals(500, profile.walletBalance)

    val paymentId = "pay_test_rzp_999888"
    val depositAmount = 250
    val result = repo.addMoneyFromRazorpay(depositAmount, paymentId)
    assertTrue(result.isSuccess)

    val userSnap = firestore.collection("users").document(uid).get().await()
    val updatedBalance = (userSnap.getLong("walletBalance") ?: 0L).toInt()
    assertEquals(750, updatedBalance)

    val cleanId = paymentId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
    val txSnap = firestore.collection("users").document(uid).collection("transactions").document("tx_rzp_$cleanId").get().await()
    assertTrue(txSnap.exists())
    assertEquals(250, (txSnap.getLong("amount") ?: 0L).toInt())
    assertEquals(true, txSnap.getBoolean("isCredit"))
    assertEquals(paymentId, txSnap.getString("paymentId"))
    assertEquals("Razorpay Standard", txSnap.getString("gateway"))
  }
}

