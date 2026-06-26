import test from 'node:test'
import assert from 'node:assert/strict'
import {
  formatScheduleIntervalMinutes,
  getSourceScheduleFormHint,
  getSourceScheduleStatusLabel,
} from '../src/features/sources/schedule.js'

test('formatScheduleIntervalMinutes formats known schedule intervals', () => {
  assert.equal(formatScheduleIntervalMinutes(15), '15 минут')
  assert.equal(formatScheduleIntervalMinutes(1440), '1 день')
  assert.equal(formatScheduleIntervalMinutes(10080), '1 неделя')
})

test('formatScheduleIntervalMinutes falls back for custom interval values', () => {
  assert.equal(formatScheduleIntervalMinutes(120), '120 мин')
})

test('source schedule labels reflect enabled and disabled states', () => {
  assert.equal(getSourceScheduleStatusLabel(true), 'Автосбор включён')
  assert.equal(getSourceScheduleStatusLabel(false), 'Автосбор выключен')
})

test('source schedule form hint explains manual and automatic modes', () => {
  assert.match(getSourceScheduleFormHint(false), /вручную/i)
  assert.match(getSourceScheduleFormHint(true), /автоматически/i)
})
