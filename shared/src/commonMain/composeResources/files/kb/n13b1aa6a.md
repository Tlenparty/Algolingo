```kotlin
  
fun main() {  
    val scope = CoroutineScope(EmptyCoroutineContext)  
    runBlocking {  
        val first = scope.async {  
            delay(100)  
            println("1")  
            "First"  
        }  
  
        val second = scope.async {  
            delay(400)  
            println("2")  
            "Second"  
        }  
        delay(300)  
        println("3")  
        println("4 + ${first.await()} +${second.await()}")  
    }  
}
```
ОТВЕТ!

1
3
2
4 + First +Second

```kotlin
fun main() {  
    val scope = CoroutineScope(EmptyCoroutineContext)  
  
    runBlocking {  
        val first = scope.async {  
            delay(100)  
            println("1")  
            throw Exception()  
            "First"  
        }  
  
        val second = scope.async {  
            delay(400)  
            println("2")  
            "Second"  
        }  
        delay(300)  
        println("3")  
        println("4 + ${first.await()} +${second.await()}")  
    }  
}
```

> Если закомментировать await то краша не будет и будет так же 1 и 3 вывод. (Будет CancellationException). Нужен SuperVisorJob()
   Если сделать launch {} будет выведено 1 и краш

Вывод
```kotlin
1
3
Exception in thread "main" java.lang.Exception
	at com.example.mytestapplication.Test2Kt$main$1$first$1.invokeSuspend(Test2.kt:18)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
	at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:584)
	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:793)
	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:697)
	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:684)

```


Что будет выведено ??
```kotlin
@OptIn(DelicateCoroutinesApi::class)
class WBTests {

    // Задача №1. Что будет выведено?
    @Test
    fun wildberriesStructurredConcurrency() {
        val coroutineContext = Job() + Dispatchers.Default
        val coroutineScope = CoroutineScope(coroutineContext)

        coroutineScope.launch { // корневая
            val request = launch { // request
                GlobalScope.launch { // GS, не наследует Job от корневого
                    delay(1000)
                    println("1") // точно будет выведено
                }

                launch { // Внутренняя
                    delay(100)
                    println("2")
                    delay(1000)
                    println("3")
                }
            }

            delay(500)
            request.cancel()
            delay(1000)
            println("4")
        }



        runBlocking {
            delay(3000L)
            coroutineScope.coroutineContext.job.cancelAndJoin()
        }
    }
}
```

**Что произойдет из-за request.cancel()?**


При отмене request отменятся все его дочерние корутины, **кроме GlobalScope.launch, так как он работает вне request**.



**Итоговая последовательность вывода:**

1. "2" (через **100мс**, до отмены request).
2. "1" (через **1000мс**, потому что GlobalScope.launch не отменился).
3. "4" (через **1500мс** — 500мс задержки + 1000мс после отмены).


⚠️ "3" **не выведется**, так как корутина, его печатающая, была отменена

Итого

```kotlin
2  
1  
4 
```


КАК решить такую задачу?? 


Задача №3

есть TextView. Требуется написать таймер — в TextView будет раз в секунду меняться текст от 5 до 1. При достижении нуля открыть SecondActivity

```kotlin
class CountdownActivity : AppCompatActivity() {

    private val countdownViewModel: CountdownViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countdown)

        val textView = findViewById<TextView>(R.id.textview)
    }

    private fun launchSecondActivity() {
        val intent = Intent(this, SecondActivity::class.java)
        startActivity(intent)
        finish()
    }
}
```


```kotlin
package com.offerfactory.video3

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// есть TextView.
// Требуется написать таймер — в TextView будет раз в секунду меняться текст от 5 до 1.
// При достижении нуля открыть SecondActivity
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.textView)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timerFlow.collect { timerValue ->
                    textView.text = timerValue.toString()
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stopTimerFlow.collect {
                    launchSecondActivity()
                }
            }
        }

        if (savedInstanceState == null) {
            viewModel.startTimer()
        }
    }

    private fun launchSecondActivity() {
        val intent = Intent(this, SecondActivity::class.java)
        startActivity(intent)
        finish()
    }
}

class MainViewModel : ViewModel() {

    private val timerFlowMutable = MutableStateFlow(5)
    val timerFlow = timerFlowMutable.asStateFlow()

    private val stopTimerFlowMutable = MutableSharedFlow<Unit>()
    val stopTimerFlow = stopTimerFlowMutable.asSharedFlow()

    fun startTimer() {
        viewModelScope.launch {
            for (i in 5 downTo 1) {
                timerFlowMutable.emit(i)
                delay(1000)
            }

            delay(1000)
            stopTimerFlowMutable.emit(Unit)
        }

    }

}


import androidx.lifecycle.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CountdownViewModel : ViewModel() {

    private val _countdown = MutableStateFlow(5)
    val countdown: MutableStateFlow = _countdown

    init {
        startCountdown()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            for (i in 5 downTo 1) {
                _countdown.emit(i)
                delay(1000)
            }
            _countdown.emit(i) // Отправляем 0, чтобы активность знала, что нужно переходить
        }
    }
}
```

```kotlin
class CountdownActivity : AppCompatActivity() {

    private val countdownViewModel: CountdownViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countdown)

        val textView = findViewById<TextView>(R.id.textview)

        // Подписываемся на изменения таймера
      lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timerFlow.collect { timerValue ->
		            textView.text = value.toString()
		            if (timerValue == 0) {
		                launchSecondActivity()
		            }
        }
    }

    private fun launchSecondActivity() {
        val intent = Intent(this, SecondActivity::class.java)
        startActivity(intent)
        finish()
    }
}
```






