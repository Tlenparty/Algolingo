**Как работает RecyclerView?**

RecyclerView работает на основе следующих ключевых компонентов:

1. **ViewHolder**:

• Представляет отдельный элемент списка.
• Хранит ссылки на виджеты (например, TextView, ImageView) для предотвращения повторного поиска этих виджетов через findViewById.
• Реализуется как внутренний класс адаптера.

2. **Adapter**:

• Адаптер связывает данные с ViewHolder.
• Определяет, как данные будут отображаться.
• Отвечает за создание новых ViewHolder и привязку данных к ним.

3. **LayoutManager**:

• Отвечает за размещение элементов на экране.
• Располагает элементы в списке (линейно, в виде сетки или настраиваемым способом).
• Основные типы:
• LinearLayoutManager — список.
• GridLayoutManager — сетка.
• StaggeredGridLayoutManager — сетка с элементами разной высоты.

4. **Recycler**:

• Хранит ViewHolder для повторного использования.
• Если элемент удаляется с экрана, его ViewHolder возвращается в пул, чтобы быть использованным повторно.
**Основной процесс работы**

1. **Повторное использование элементов:**

• Когда пользователь прокручивает список, элементы, выходящие за пределы видимости, не уничтожаются. Вместо этого они возвращаются в пул Recycler.
==Если обернуть recycler в scrollview то ресайклер выгрузит все айтемы ==

> **📌 Что произойдёт?**
RecyclerView потеряет свою “ленивую” подгрузку (Lazy Loading):**
• **RecyclerView** построен так, чтобы подгружать только видимые элементы, а остальные создавать или “перепользовать” только по мере прокрутки.
• При оборачивании в **ScrollView** он загрузит **все элементы сразу**, потому что **ScrollView** требует знать полный размер контента.
**Из-за этого:**
• Значительно возрастёт нагрузка на память.
• Увеличится время прорисовки списка.
• Возможны тормоза (особенно если список длинный).
• Эти элементы перерабатываются (рециклируются) для отображения новых данных, вместо создания новых объектов.

2. **Binding данных:**

• Для каждого видимого элемента адаптер предоставляет данные, которые привязываются к ViewHolder через метод onBindViewHolder().

3. **Создание ViewHolder:**

• Если Recycler не может найти ViewHolder в своем пуле (например, при первом отображении списка), адаптер создает новый ViewHolder через метод onCreateViewHolder().

4. **Размещение элементов:**

• LayoutManager определяет, как элементы располагаются на экране (например, вертикально или в сетке).
• При изменении ориентации или размеров экрана LayoutManager пересчитывает расположение элементов.

**Основной код RecyclerView**

**Пример:**

1. **Макет для элемента списка (XML):**
```xml
<!-- item_layout.xml -->
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/textView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="16sp"/>
</LinearLayout>
```

2. **Адаптер:**
```kotlin
class MyAdapter(private val items: List<String>) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    // ViewHolder для одного элемента списка
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
    }

    // Создание ViewHolder (вызовется при отсутствии доступного ViewHolder в пуле)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(view)
    }

    // Привязка данных к ViewHolder
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textView.text = items[position]
    }

    // Количество элементов в списке
    override fun getItemCount(): Int = items.size
}
```

3. **Activity или Fragment:**
```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this) // Горизонтальная прокрутка — LinearLayoutManager.HORIZONTAL
        recyclerView.adapter = MyAdapter(listOf("Item 1", "Item 2", "Item 3"))
    }
}
```

**Особенности и преимущества RecyclerView**

1. **Производительность:**

• Использует переработку (рециклинг) ViewHolder, что снижает нагрузку на память и процессор.
• Работает лучше, чем ListView или GridView для больших наборов данных.

2. **Гибкость:**

• Поддержка различных типов макетов (списки, сетки, карусели).
• Легкость кастомизации (например, создание пользовательского LayoutManager).

3. **Анимации:**

• Встроенная поддержка анимаций добавления, удаления и перемещения элементов.

4. **Разделители и отступы:**

• Можно добавлять через ItemDecoration, вместо необходимости изменять макеты элементов.

**Как работает асинхронное обновление данных в RecyclerView:**

1. **Загрузка данных в фоновом потоке**
2. **Обновление адаптера на главном потоке**
3. **Использование методов адаптера для обновления данных**
4. **Плавное обновление данных с помощью** notifyItemChanged, notifyDataSetChanged, и других методов

**1. Загрузка данных в фоновом потоке (например, с использованием корутин или AsyncTask)**
**2. Обновление адаптера на главном потоке**
RecyclerView и его адаптер должны обновляться на главном потоке. Android не позволяет напрямую изменять UI из фонового потока, поэтому важно использовать механизмы, такие как withContext(Dispatchers.Main) или runOnUiThread(), чтобы обновить данные.

**Пример использования метода** submitList **в** ListAdapter:

Если ваш адаптер является экземпляром ListAdapter, то лучший способ обновить данные — это использовать метод submitList(). Этот метод автоматически запускает анимацию обновления списка.
```kotlin
val adapter = MyListAdapter()
adapter.submitList(newData)
```

ListAdapter управляет асинхронным обновлением списка, так что даже если данные меняются часто, интерфейс остается плавным и не блокируется.

**3. Использование методов адаптера для обновления данных**

Если вы используете обычный адаптер (RecyclerView.Adapter), вам нужно будет вручную уведомить адаптер о том, что данные изменились. Это можно сделать с помощью различных методов уведомлений.

• notifyDataSetChanged() — обновляет весь список, но может быть менее эффективным, поскольку пересоздает все элементы.
• notifyItemInserted(position) — уведомляет об добавлении нового элемента.
• notifyItemRemoved(position) — уведомляет о удалении элемента.
• notifyItemChanged(position) — уведомляет о изменении конкретного элемента на определенной позиции.

**4. Плавное обновление с помощью DiffUtil**
Для более эффективного обновления списка данных в RecyclerView можно использовать класс DiffUtil. Этот класс вычисляет разницу между старым и новым списками, минимизируя количество изменений в UI, чтобы обновление происходило плавно.

**Пример с использованием** DiffUtil **и** ListAdapter:
```kotlin
class MyListAdapter : ListAdapter<Item, MyViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        // Создание ViewHolder
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getItem(position)
        // Привязка данных
    }

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            // Сравниваем уникальные идентификаторы элементов
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            // Сравниваем содержимое элементов
            return oldItem == newItem
        }
    }
}
```

• ListAdapter автоматически вызывает DiffUtil для расчета изменений, минимизируя перерисовку элементов.

• Вызывая submitList(newList), вы можете передать новый список, и DiffUtil сам решит, какие элементы нужно добавить, удалить или изменить.

### **Как работает DiffUtil?**

DiffUtil использует два основных метода:

1. areItemsTheSame(oldItem, newItem) — проверяет, являются ли два элемента из старого и нового списка одним и тем же элементом (например, по уникальному идентификатору).

2. areContentsTheSame(oldItem, newItem) — проверяет, одинаковы ли содержимое двух элементов (например, текст или изображение).


Затем, на основе этих проверок, DiffUtil вычисляет, какие элементы были добавлены, удалены или изменены, и оптимально применяет эти изменения к **RecyclerView**.

**Алгоритм работы DiffUtil**

1. **Подготовка данных**:

• У вас есть два списка: старый (перед изменениями) и новый (с изменениями).
• DiffUtil получает эти два списка и начинает их сравнение.

2. **Сравнение элементов**:

• Для каждого элемента из старого списка DiffUtil вызывает метод areItemsTheSame(), чтобы проверить, являются ли элементы одинаковыми. Это важно для того, чтобы понять, был ли элемент перемещен или изменен, или его нужно удалить и добавить новый.
• Если элементы одинаковы, то далее вызывается метод areContentsTheSame(), чтобы проверить, были ли данные (содержимое) элемента изменены.

3. **Создание списка изменений**:

• После вычисления разницы между старым и новым списком, DiffUtil генерирует список изменений (добавление, удаление, изменение элементов) и передает его адаптеру для обновления интерфейса.

4. **Применение изменений**:

• Изменения применяются к адаптеру с использованием таких методов, как notifyItemChanged(), notifyItemInserted() и notifyItemRemoved(), что позволяет обновить только измененные элементы без перерисовки всего списка.

**Пример использования DiffUtil:**


Предположим, у нас есть список объектов Item, и мы хотим обновить этот список в адаптере.

1. **Создаем класс, который наследуется от** DiffUtil.ItemCallback:
```kotlin
class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {

    // Проверка, являются ли два элемента одинаковыми
    override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem.id == newItem.id  // предполагается, что у Items есть уникальный id
    }

    // Проверка, одинаковы ли содержимое элементов
    override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem == newItem  // предполагается, что в классе Item переопределен метод equals()
    }
}
```

2. **Использование** ListAdapter **и** submitList() **для асинхронного обновления списка:**

```kotlin
class ItemAdapter : ListAdapter<Item, ItemViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        // Создание ViewHolder
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)  // Привязка данных к View
    }
}
```

3. **Обновление списка с использованием** submitList():
```kotlin
val adapter = ItemAdapter()

// Новые данные для списка
val newList = listOf(
    Item(1, "Item 1"),
    Item(2, "Item 2 Updated"),  // Измененный элемент
    Item(3, "Item 3")
)

// Обновление данных с использованием DiffUtil
adapter.submitList(newList)
```

Метод submitList() в ListAdapter автоматически использует DiffUtil для вычисления разницы между старым и новым списком, и применяет только необходимые изменения.

**Важные моменты:**

• **Эффективность**: Использование DiffUtil позволяет значительно уменьшить количество операций перерисовки элементов в RecyclerView, так как он только обновляет те элементы, которые действительно изменились.

• **Уменьшение количества анимаций**: Вместо того чтобы обновлять весь список, можно применять только минимальные изменения, что делает обновление плавным и менее ресурсоемким.

• **Использование с** ListAdapter: ListAdapter — это специальный адаптер для RecyclerView, который автоматически использует DiffUtil для вычисления разницы между списками, что упрощает код и делает его более читаемым.

**Пример с использованием DiffUtil в адаптере без ListAdapter:**
Если вы не хотите использовать ListAdapter, вы можете вручную вызвать DiffUtil.calculateDiff(), чтобы рассчитать разницу и обновить RecyclerView.
```kotlin
class MyAdapter : RecyclerView.Adapter<MyViewHolder>() {

    private var items: List<Item> = emptyList()

    fun updateItems(newItems: List<Item>) {
        val diffResult = DiffUtil.calculateDiff(ItemDiffCallback(items, newItems))
        items = newItems
        diffResult.dispatchUpdatesTo(this)  // Обновляем адаптер с минимальными изменениями
    }

    // Прочие методы адаптера...
}
```

В этом случае вам нужно вручную передать старый и новый список в DiffUtil.calculateDiff(), а затем применить изменения с помощью dispatchUpdatesTo().

**Заключение**

• DiffUtil является очень мощным инструментом для эффективного обновления данных в **RecyclerView**.

• Он помогает минимизировать количество изменений UI, обновляя только те элементы, которые были изменены, добавлены или удалены, улучшая производительность.

• DiffUtil используется с ListAdapter, но также может быть использован вручную с обычными адаптерами, что делает его универсальным решением для асинхронных обновлений данных в Android-приложениях.

> [!warning] Две разные «Callback» — не путать
> - **`DiffUtil.ItemCallback<T>`** — для `ListAdapter` (`submitList` сам считает diff в фоне). Методы: `areItemsTheSame`, `areContentsTheSame`.
> - **`DiffUtil.Callback`** — для ручного `DiffUtil.calculateDiff(...)`; у него **есть конструктор со списками** и методы `getOldListSize()`/`getNewListSize()` + те же два сравнения. В примере с ручным `calculateDiff(ItemDiffCallback(items, newItems))` должен использоваться именно `DiffUtil.Callback`, а не `ItemCallback`.

> [!tip] Что важно на собесе про RecyclerView
> - **Почему быстрее ListView:** переиспользование `ViewHolder` (`RecycledViewPool`) + обязательный паттерн ViewHolder (нет повторных `findViewById`). Подробнее — [[Recycler. ViewPool, PayLoads]].
> - **`notifyDataSetChanged()` — плохо:** перерисовывает весь список без анимаций. Предпочитай `ListAdapter` + `submitList` (diff в фоне) или точечные `notifyItemXxx`.
> - **Стабильные ключи обязательны:** `areItemsTheSame` по `id`; иначе анимации перестановки и сохранение состояния item ломаются. То же, что `key` в [[Compose. Slot table|LazyColumn]].
> - **Частичное обновление** одного поля item — через **payload** (`getChangePayload` + `onBindViewHolder(holder, position, payloads)`), чтобы не перебиндивать весь холдер. См. [[Recycler. ViewPool, PayLoads]].
> - **Не оборачивай RecyclerView в ScrollView** — теряется переработка (создаются все элементы сразу). Для смешанного контента — несколько viewType или `ConcatAdapter`.

Связано: [[Recycler. ViewPool, PayLoads]], [[Compose. Slot table]], [[Структуры данных]], [[Custom view]]