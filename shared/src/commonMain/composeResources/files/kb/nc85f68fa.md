```kotlin
import android.annotation.SuppressLint  
import android.content.Context  
import androidx.lifecycle.MutableLiveData  
import androidx.lifecycle.ViewModel  
import com.example.jetnews.R  
import kotlinx.coroutines.Dispatchers  
import kotlinx.coroutines.GlobalScope  
import kotlinx.coroutines.flow.Flow  
import kotlinx.coroutines.flow.filter  
import kotlinx.coroutines.flow.flatMapConcat  
import kotlinx.coroutines.flow.flowOf  
import kotlinx.coroutines.flow.flowOn  
import kotlinx.coroutines.flow.onEach  
import kotlinx.coroutines.flow.zip  
import kotlinx.coroutines.launch  
  
@SuppressLint("StringFormatInvalid")  
class ScreenViewModel(  
    var configs: ConfigRepository, // 1 Хочу здесь интерактор с  USECASES  
    var holder: UserHolder,  
    var cards: CardRepository,  
    var analytics: AnalyticsService,  
    var context: Context,  
) : ViewModel() {  
  
    lateinit var config: Config  
    lateinit var card: CardType  
  
    var liveData = MutableLiveData<UiModel>()  
    var succesFulChecks = 0L  
  
    init {  
        GlobalScope.launch {  
            flowOf(configs.loadConfig()).zip(cards.observeAvailableCards(), { it1, it2 -> Pair(it1, it2) })  
                .flowOn(Dispatchers.Default)  
                .filter {  
                    it.second == CardType.PREMIUM && holder.getUserScore() > 42  
                            || it.second != CardType.PREMIUM  
                            || it.first.specialUsers.contains(holder.getUserId())  
                }  
                .onEach {  
                    config = it.first  
                    card = it.second  
                }  
                .flatMapConcat {  
                    flowOf(configs.checkBlackList(it.second, holder.getUserId())).apply {  
                        succesFulChecks++  
                    }  
                }                .collect {  
                    liveData.value = UiModel(  
                        title = if (card == CardType.CREDIT) config.creditTitle  
                        else if (card == CardType.DEBIT) config.debitTitle  
                        else if (card == CardType.KID) config.kidTitle  
                        else if (card == CardType.PREMIUM) context.getString(R.string.app_name)  
                        else "",  
                        description = context.getString(R.string.app_name, config.kinderSurprizeCashBack)  
                    )  
                }  
        }    }  
}  
  
interface ConfigRepository {  
    suspend fun loadConfig(): Config  
    fun checkBlackList(cardType: CardType, userId: Long): BlackList  
}  
  
interface AnalyticsService {  
    //@FormUrlEncoded  
    //@POST ("v1/analytics")    suspend fun sendSuccessfulChecks(checks: Long)  
}  
  
interface UserHolder {  
    fun getUserId(): Long  
    fun getUserScore(): Long  
}  
  
interface CardRepository {  
    fun observeAvailableCards(): Flow<CardType>  
}  
  
interface BlackList {  
    var successFulChecks: Long  
}  
  
interface Config {  
    val debitTitle: String  
    val creditTitle: String  
    val kidTitle: String  
    val kinderSurprizeCashBack: Long  
    val specialUsers: List<Long>  
}  
  
enum class CardType {  
    DEBIT, CREDIT, KID, PREMIUM  
}  
  
class UiModel(val title: String, val description: String)
```

```kotlin
import android.annotation.SuppressLint  
import android.content.Context  
import androidx.annotation.StringRes  
import androidx.lifecycle.LiveData  
import androidx.lifecycle.MutableLiveData  
import androidx.lifecycle.ViewModel  
import androidx.lifecycle.viewModelScope  
import com.example.jetnews.R  
import kotlinx.coroutines.Deferred  
import kotlinx.coroutines.Dispatchers  
import kotlinx.coroutines.async  
import kotlinx.coroutines.flow.Flow  
import kotlinx.coroutines.flow.filter  
import kotlinx.coroutines.flow.flowOn  
import kotlinx.coroutines.flow.onEach  
import kotlinx.coroutines.launch  
  
private const val USERSCORE = 42  
// 1 Переименовать, непонятно что за screen  
// 3 Почему не applicationContext ??  
@SuppressLint("StringFormatInvalid")  
class ScreenViewModel(  
    val configs: ConfigRepository, // 2 Хочу здесь интерактор с USECASES  
    val holder: UserHolder,  
    val cards: CardRepository,  
    val analytics: AnalyticsService,  
    val textProvider: TextProvider,  
) : ViewModel() {  
  
    // 4 убрать lateinit  
    //  lateinit var config: Config    // lateinit var card: CardType  
    // Убрал снаружи liveData    private val _liveData = MutableLiveData<UiModel>()  
    val liveData: LiveData<UiModel> = _liveData  
  
//    // 5 Синхронизация?  
//    var successFulChecks = 0L  
  
    // Вроде ок    // Вытащить в USECASE.    init {  
        // Не привязанк жц  
        viewModelScope.launch {  
            val loadedConfid = async { configs.loadConfig() }  
  
            cards.observeAvailableCards()  
                .flowOn(Dispatchers.Default)  
                .filter {  
                    applyFilter(it, loadedConfid)  
                }  
                .onEach {  
                    configs.checkBlackList(it, holder.getUserId())  
                        .successFulChecks()  
                }  
                .collect {  
                    _liveData.value = UiModel(  
                        title = when (it) {  
                            CardType.CREDIT -> loadedConfid.await().creditTitle  
                            CardType.DEBIT -> loadedConfid.await().debitTitle  
                            CardType.KID -> loadedConfid.await().kidTitle  
                            CardType.PREMIUM -> textProvider.getString(R.string.app_name)  
                        },  
                        description = textProvider.getString(R.string.app_name, loadedConfid.await().kinderSurprizeCashBack)  
                    )  
                }  
        }    }  
  
    // Вынести 42 в константу  
    private suspend fun applyFilter(it: CardType, config: Deferred<Config>) =  
        (  
            it == CardType.PREMIUM && holder.getUserScore() > USERSCORE ||  
                it != CardType.PREMIUM || holder.getUserId() in config.await().specialUsers  
            )  
  
}  
  
// Domain impl - в data  
interface ConfigRepository {  
    // Разделение интерфейсов  
    suspend fun loadConfig(): Config  
    fun checkBlackList(cardType: CardType, userId: Long): BlackList  
}  
  
interface AnalyticsService {  
    // @FormUrlEncoded  
    // @POST ("v1/analytics")    suspend fun sendSuccessfulChecks(checks: Long)  
}  
  
// Domain  
interface UserHolder {  
    fun getUserId(): Long  
    fun getUserScore(): Long  
}  
  
// Domain реализация в Data  
interface CardRepository {  
    fun observeAvailableCards(): Flow<CardType>  
}  
  
interface BlackList {  
    fun successFulChecks()  
}  
  
// Core.Utils  
interface TextProvider {  
    // тестирвоание в моках  
    fun getString(@StringRes id: Int): String  
    fun getString(@StringRes id: Int, vararg str: Any): String  
}  
  
// Core.UtilsImpl  
class TextProviderImpl(val context: Context) : TextProvider {  
    override fun getString(id: Int): String {  
        return ""  
    }  
  
    override fun getString(id: Int, vararg str: Any): String {  
        return ""  
    }  
}  
  
// Domain  
interface Config {  
    val debitTitle: String  
    val creditTitle: String  
    val kidTitle: String  
    val kinderSurprizeCashBack: Long  
    val specialUsers: List<Long>  
}  
  
// Domain  
enum class CardType {  
    DEBIT, CREDIT, KID, PREMIUM  
}  
  
// Presentation  
data class UiModel(val title: String, val description: String)

```

---

## Разбор: что не так в первом варианте

> [!danger] Критичные проблемы
> 1. **`GlobalScope.launch`** — корутина не привязана к жизненному циклу `ViewModel`. Утечка: продолжит работу после `onCleared()`, держит ссылку на `Context`. Замена — `viewModelScope`, который отменяется автоматически.
> 2. **`context: Context` во ViewModel** — прямая ссылка на Android-контекст в presentation-слое. Утечка + невозможность юнит-тестов. Спрятано за абстракцию `TextProvider` (мокается в тестах).
> 3. **`MutableLiveData` наружу** — любой может писать в `liveData` извне. Наружу отдаётся неизменяемый `LiveData`, мутабельный `_liveData` приватный.
> 4. **`lateinit var config/card` как поля** — общее мутабельное состояние между эмиссиями Flow → гонки. Убрано, значения берутся локально через `loadedConfig.await()`.

> [!warning] Логические и стилистические замечания
> • **`var` в конструкторе** заменены на `val` — зависимости не должны переприсваиваться.
> • **`succesFulChecks++`** (с опечаткой) — немотивированный счётчик-side-effect внутри `flatMapConcat`, к тому же без синхронизации. Удалён.
> • **Цепочка `if/else if` по `CardType`** заменена на `when`, который компилятор проверяет на полноту (exhaustive) — забытую ветку enum поймает компиляция, а не прод.
> • **Магическое число `42`** вынесено в константу `USERSCORE`.
> • **`flowOf(configs.loadConfig())`** — `suspend`-вызов выполнялся синхронно при создании Flow. Заменено на `async { … }`, чтобы конфиг грузился параллельно и доставался лениво через `await()`.
> • **Разделение слоёв** — комментарии `// Domain`, `// Data`, `// Core` фиксируют, куда вынести интерфейсы и реализации (Clean Architecture). Интерактор/`UseCase` вместо репозиториев напрямую во ViewModel.

> [!tip] На что смотрит ревьюер на собесе
> Главное — назвать **утечку через `GlobalScope` + `Context`** и **публичный `MutableLiveData`**: это «красные флаги» уровня архитектуры. Остальное (магические числа, `when` вместо `if`, именование) — приятный бонус, показывающий внимательность.

Связано: [[Coroutines]], [[FLOW]], [[Чистая архитектура]], [[ViewModel, SaveStateHandle, SavedStateRegistryOwner]], [[SOLID]]