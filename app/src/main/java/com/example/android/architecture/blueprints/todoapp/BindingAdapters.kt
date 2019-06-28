package com.example.android.architecture.blueprints.todoapp

import androidx.core.widget.ContentLoadingProgressBar
import androidx.databinding.BindingAdapter


@BindingAdapter("loading")
fun ContentLoadingProgressBar.contentLoadingProgressBar(loading: Boolean) {

    if (loading) {
        this.show()
    } else {
        this.hide()
    }
}
