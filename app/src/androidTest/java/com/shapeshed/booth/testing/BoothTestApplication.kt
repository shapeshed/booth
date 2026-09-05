package com.shapeshed.booth.testing

import com.shapeshed.booth.BoothApp
import dagger.hilt.android.testing.CustomTestApplication

@CustomTestApplication(BoothApp::class)
interface BoothTestApplication
