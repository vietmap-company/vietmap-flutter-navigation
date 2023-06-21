package com.example.models

class VietMapNavigationOptions private constructor() {
    var isCustomizeUI:Boolean =false
    companion object {
        private var _instance = VietMapNavigationOptions()
        val instance: VietMapNavigationOptions
            get() = synchronized(this) {
                _instance
            }
    }
}