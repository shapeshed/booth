package com.shapeshed.booth.testing

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

/** Prevents instrumented tests from targeting the normal developer app install. */
class BoothTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle) {
        super.onCreate(arguments)
        check(targetContext.packageName == TEST_APPLICATION_ID) {
            "Booth instrumented tests must target $TEST_APPLICATION_ID, " +
                "but targeted ${targetContext.packageName}."
        }
    }

    private companion object {
        const val TEST_APPLICATION_ID = "com.shapeshed.booth.deviceTest"
    }
}
