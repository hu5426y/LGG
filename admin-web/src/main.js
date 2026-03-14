import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  ElButton,
  ElDatePicker,
  ElForm,
  ElInput,
  ElInputNumber,
  ElOption,
  ElSelect,
  ElSpace,
  ElTable,
  ElTableColumn
} from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.component('ElButton', ElButton)
app.component('ElDatePicker', ElDatePicker)
app.component('ElForm', ElForm)
app.component('ElInput', ElInput)
app.component('ElInputNumber', ElInputNumber)
app.component('ElOption', ElOption)
app.component('ElSelect', ElSelect)
app.component('ElSpace', ElSpace)
app.component('ElTable', ElTable)
app.component('ElTableColumn', ElTableColumn)
app.mount('#app')
