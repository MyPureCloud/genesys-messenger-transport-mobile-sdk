package com.genesys.cloud.messenger.composeapp.viewmodel

import kotlin.test.Test

/**
 * Debug test to understand what's happening with the TestBedViewModel
 */
class TestBedViewModelDebugTest {

    @Test
    fun testBasicViewModelCreation() {
        val viewModel = TestBedViewModel()
        
        println("Available commands:")
        viewModel.availableCommands.value.forEach { command ->
            println("- ${command.name}")
        }
        
        println("\nInitial socket messages count: ${viewModel.socketMessages.value.size}")
        
        // Try a simple command
        viewModel.onCommandChanged("deployment")
        println("Command set to: ${viewModel.command}")
        
        viewModel.onCommandSend()
        println("After command send - messages count: ${viewModel.socketMessages.value.size}")
        println("Command waiting: ${viewModel.commandWaiting}")
        
        viewModel.socketMessages.value.forEach { message ->
            println("Message: ${message.type} - ${message.content}")
        }
    }
}