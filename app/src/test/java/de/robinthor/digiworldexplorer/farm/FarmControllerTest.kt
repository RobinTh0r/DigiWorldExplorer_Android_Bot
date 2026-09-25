package de.robinthor.digiworldexplorer.farm

import org.junit.Assert.*
import org.junit.Test

class FarmControllerTest {
    private fun field(first: PlotState, seeds: Int? = 1) =
        FarmObservation(FarmView.FIELD, listOf(first) + List(5) { PlotState.GROWING }, seeds)

    @Test fun `harvest is issued once then free planting requires selection and visual proof`() {
        val controller = FarmController()
        assertEquals(FarmOperation.HARVEST, controller.tick(field(PlotState.RIPE), 0).operation)
        assertEquals(FarmOperation.WAIT, controller.tick(field(PlotState.RIPE), 1).operation)
        assertEquals(FarmOperation.OPEN_SEEDS, controller.tick(field(PlotState.EMPTY), 2).operation)
        assertEquals(1, controller.harvested)
        val seeds = FarmObservation(FarmView.SEEDS, freeSeeds = 1, freeSlot = 0)
        assertEquals(FarmOperation.SELECT_FREE_SEED, controller.tick(seeds, 3).operation)
        assertEquals(FarmOperation.CONFIRM_SEED, controller.tick(seeds.copy(selectedSlot = 0), 4).operation)
        assertEquals(FarmOperation.COMPLETE, controller.tick(field(PlotState.GROWING, 0), 5).operation)
        assertEquals(1, controller.planted)
    }

    @Test fun `water closes once and unknown seed costs park`() {
        val controller = FarmController()
        assertEquals(FarmOperation.CLOSE_WATER, controller.tick(FarmObservation(FarmView.WATER), 0).operation)
        assertEquals(FarmOperation.WAIT, controller.tick(FarmObservation(FarmView.WATER), 1).operation)
        assertEquals(FarmOperation.PARK, controller.tick(field(PlotState.EMPTY), 2).operation)
        assertEquals(FarmOperation.WAIT, FarmController().tick(field(PlotState.EMPTY, null), 0).operation)
    }

    @Test fun `animation cannot postpone timeout forever and stop cancels pending harvest`() {
        val controller = FarmController(100)
        controller.tick(field(PlotState.RIPE), 0)
        assertEquals(FarmOperation.PARK, controller.tick(field(PlotState.RIPE).copy(stable = false), 100).operation)
        val cancelled = FarmController()
        cancelled.tick(field(PlotState.RIPE), 0)
        cancelled.cancel()
        assertEquals(FarmOperation.PARK, cancelled.tick(field(PlotState.EMPTY), 1).operation)
        assertEquals(0, cancelled.harvested)
    }

    @Test fun `bubble-covered ripe and empty plots are skipped`() {
        val controller = FarmController()
        val plots = listOf(PlotState.RIPE, PlotState.EMPTY) + List(4) { PlotState.GROWING }
        val command = controller.tick(
            FarmObservation(FarmView.FIELD, plots, freeSeeds = 1, blockedPlots = setOf(0)),
            0,
        )
        assertEquals(FarmOperation.OPEN_SEEDS, command.operation)
        assertEquals(1, command.plot)
    }

    @Test fun `already selected free slot goes directly to confirmation`() {
        val controller = FarmController()
        assertEquals(FarmOperation.OPEN_SEEDS, controller.tick(field(PlotState.EMPTY), 0).operation)
        val seeds = FarmObservation(FarmView.SEEDS, freeSlot = 0, selectedSlot = 0)
        assertEquals(FarmOperation.CONFIRM_SEED, controller.tick(seeds, 1).operation)
    }

    @Test fun `explicit free slot tap can be confirmed when animated outline is unreadable`() {
        val controller = FarmController()
        assertEquals(FarmOperation.OPEN_SEEDS, controller.tick(field(PlotState.EMPTY), 0).operation)
        val seeds = FarmObservation(FarmView.SEEDS, freeSlot = 0, selectedSlot = null)
        assertEquals(FarmOperation.SELECT_FREE_SEED, controller.tick(seeds, 1).operation)
        assertEquals(FarmOperation.CONFIRM_SEED, controller.tick(seeds, 2).operation)
    }

    @Test fun `locked plots are safely ignored instead of parking the whole farm`() {
        val controller = FarmController()
        val plots = listOf(PlotState.LOCKED, PlotState.RIPE) + List(4) { PlotState.GROWING }
        val command = controller.tick(FarmObservation(FarmView.FIELD, plots, freeSeeds = 0), 0)
        assertEquals(FarmOperation.HARVEST, command.operation)
        assertEquals(1, command.plot)
    }

    @Test fun `unknown plot is skipped while a verified neighbour can be planted`() {
        val controller = FarmController()
        val plots = listOf(PlotState.UNKNOWN, PlotState.EMPTY) + List(4) { PlotState.GROWING }
        val command = controller.tick(FarmObservation(FarmView.FIELD, plots, freeSeeds = 1), 0)
        assertEquals(FarmOperation.OPEN_SEEDS, command.operation)
        assertEquals(1, command.plot)
    }

    @Test fun `verified planted plot may animate to unknown without blocking completion`() {
        val controller = FarmController()
        assertEquals(FarmOperation.OPEN_SEEDS, controller.tick(field(PlotState.EMPTY), 0).operation)
        val dialog = FarmObservation(FarmView.SEEDS, freeSlot = 0, selectedSlot = 0)
        assertEquals(FarmOperation.CONFIRM_SEED, controller.tick(dialog, 1).operation)
        assertEquals(FarmOperation.COMPLETE, controller.tick(field(PlotState.GROWING, 0), 2).operation)
        assertEquals(FarmOperation.COMPLETE, controller.tick(field(PlotState.UNKNOWN, 0), 3).operation)
    }

    @Test fun `seed priority is right then middle then common`() {
        val controller = FarmController()
        val start = field(PlotState.EMPTY, seeds = 6).copy(seedCounts = listOf(5, 1, 0))
        assertEquals(FarmOperation.OPEN_SEEDS, controller.tick(start, 0).operation)
        val dialog = FarmObservation(FarmView.SEEDS, freeSlot = 1, seedCounts = listOf(5, 1, 0))
        val command = controller.tick(dialog, 1)
        assertEquals(FarmOperation.SELECT_FREE_SEED, command.operation)
        assertEquals(1, command.slot)
    }

    @Test fun `watering chooses purple priority and ads require pass`() {
        val plots = List(6) { PlotState.GROWING }
        val waterable = mapOf(0 to 1, 2 to 3, 4 to 2)
        val enabled = FarmObservation(FarmView.FIELD, plots, freeSeeds = 0, wateringCans = 1,
            wateringPriorities = waterable, wateringEnabled = true)
        assertEquals(2, FarmController().tick(enabled, 0).plot)
        val noCan = enabled.copy(wateringCans = 0)
        assertEquals(FarmOperation.COMPLETE, FarmController().tick(noCan, 0).operation)
        assertEquals(FarmOperation.OPEN_WATER, FarmController().tick(noCan.copy(adSkipPass = true), 0).operation)
    }

    @Test fun `complete is an idle result and newly ripe plots are checked later`() {
        val controller = FarmController()
        assertEquals(FarmOperation.COMPLETE, controller.tick(field(PlotState.GROWING, 0), 0).operation)
        assertEquals(FarmOperation.HARVEST, controller.tick(field(PlotState.RIPE, 0), 1).operation)
    }

    @Test fun `late confirming frame wins over timeout`() {
        val controller = FarmController(100)
        assertEquals(FarmOperation.HARVEST, controller.tick(field(PlotState.RIPE), 0).operation)
        assertEquals(FarmOperation.OPEN_SEEDS, controller.tick(field(PlotState.EMPTY), 100).operation)
    }
}
