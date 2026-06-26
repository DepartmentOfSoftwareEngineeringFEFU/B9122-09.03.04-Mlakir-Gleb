import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createSourceSchema,
  defaultCreateSourceValues,
  getSourceUrlHelpText,
  getUpdateSourceFormValues,
  mapCreateSourceFormToDto,
  mapUpdateSourceFormToDto,
} from '../src/features/sources/form.js'
import { sourceTypeOptions } from '../src/lib/constants.js'

test('createSourceSchema allows disabled auto collection without interval', () => {
  const result = createSourceSchema.safeParse({
    ...defaultCreateSourceValues,
    organizationId: 1,
    name: 'Импорт отзывов',
    scheduleEnabled: false,
    scheduleIntervalMinutes: null,
  })

  assert.equal(result.success, true)
  if (result.success) {
    assert.equal(result.data.scheduleIntervalMinutes, null)
  }
})

test('createSourceSchema requires interval when auto collection is enabled', () => {
  const result = createSourceSchema.safeParse({
    ...defaultCreateSourceValues,
    organizationId: 1,
    name: 'Tabiturient',
    type: 'TABITURIENT',
    baseUrl: 'https://tabiturient.ru/vuzu/dvfu/',
    scheduleEnabled: true,
    scheduleIntervalMinutes: null,
  })

  assert.equal(result.success, false)
  if (!result.success) {
    assert.equal(result.error.issues[0]?.path[0], 'scheduleIntervalMinutes')
  }
})

test('sourceTypeOptions includes OTZOVIK for source type select', () => {
  assert.equal(sourceTypeOptions.some((item) => item.value === 'OTZOVIK'), true)
})

test('sourceTypeOptions includes VUZOPEDIA for source type select', () => {
  assert.equal(sourceTypeOptions.some((item) => item.value === 'VUZOPEDIA'), true)
})

test('getSourceUrlHelpText returns Otzovik-specific helper text', () => {
  assert.match(getSourceUrlHelpText('OTZOVIK'), /otzovik\.com\/reviews/i)
})

test('getSourceUrlHelpText returns Vuzopedia-specific helper text', () => {
  assert.match(getSourceUrlHelpText('VUZOPEDIA'), /vuzopedia\.ru\/vuz\/3281\/otziv/i)
})

test('createSourceSchema accepts valid OTZOVIK url and normalizes to https in dto', () => {
  const result = createSourceSchema.safeParse({
    ...defaultCreateSourceValues,
    organizationId: 1,
    name: 'Otzovik ДВФУ',
    type: 'OTZOVIK',
    baseUrl: 'http://otzovik.com/reviews/dalnevostochniy_federalniy_universitet_dvfu/',
    scheduleEnabled: false,
    scheduleIntervalMinutes: null,
  })

  assert.equal(result.success, true)
  if (result.success) {
    const dto = mapCreateSourceFormToDto(result.data)
    assert.equal(
      dto.baseUrl,
      'https://otzovik.com/reviews/dalnevostochniy_federalniy_universitet_dvfu/',
    )
  }
})

test('createSourceSchema accepts valid VUZOPEDIA url', () => {
  const result = createSourceSchema.safeParse({
    ...defaultCreateSourceValues,
    organizationId: 1,
    name: 'Vuzopedia ДВФУ',
    type: 'VUZOPEDIA',
    baseUrl: 'https://vuzopedia.ru/vuz/3281/otziv',
    scheduleEnabled: false,
    scheduleIntervalMinutes: null,
  })

  assert.equal(result.success, true)
  if (result.success) {
    const dto = mapCreateSourceFormToDto(result.data)
    assert.equal(dto.baseUrl, 'https://vuzopedia.ru/vuz/3281/otziv')
  }
})

test('createSourceSchema rejects invalid OTZOVIK url with dedicated message', () => {
  const result = createSourceSchema.safeParse({
    ...defaultCreateSourceValues,
    organizationId: 1,
    name: 'Otzovik ДВФУ',
    type: 'OTZOVIK',
    baseUrl: 'https://otzovik.com/company/dvfu/',
    scheduleEnabled: false,
    scheduleIntervalMinutes: null,
  })

  assert.equal(result.success, false)
  if (!result.success) {
    assert.equal(
      result.error.issues.some(
        (issue) => issue.message === 'Укажите корректную ссылку на страницу отзывов Otzovik.',
      ),
      true,
    )
  }
})

test('createSourceSchema rejects invalid VUZOPEDIA url with dedicated message', () => {
  const result = createSourceSchema.safeParse({
    ...defaultCreateSourceValues,
    organizationId: 1,
    name: 'Vuzopedia ДВФУ',
    type: 'VUZOPEDIA',
    baseUrl: 'https://vuzopedia.ru/vuz/dvfu/otziv',
    scheduleEnabled: false,
    scheduleIntervalMinutes: null,
  })

  assert.equal(result.success, false)
  if (!result.success) {
    assert.equal(
      result.error.issues.some(
        (issue) => issue.message === 'Укажите корректную ссылку на страницу отзывов Vuzopedia.',
      ),
      true,
    )
  }
})

test('mapCreateSourceFormToDto sends schedule fields and compatibility collection mode', () => {
  const dto = mapCreateSourceFormToDto({
    organizationId: 7,
    name: 'Tabiturient',
    type: 'TABITURIENT',
    baseUrl: 'https://tabiturient.ru/vuzu/dvfu/',
    scheduleEnabled: true,
    scheduleIntervalMinutes: 1440,
    collectionMode: 'MANUAL',
    description: '',
  })

  assert.deepEqual(dto, {
    organizationId: 7,
    name: 'Tabiturient',
    type: 'TABITURIENT',
    baseUrl: 'https://tabiturient.ru/vuzu/dvfu/',
    collectionMode: 'MANUAL',
    scheduleEnabled: true,
    scheduleIntervalMinutes: 1440,
    description: undefined,
  })
})

test('mapUpdateSourceFormToDto clears interval when auto collection is disabled', () => {
  const dto = mapUpdateSourceFormToDto({
    organizationId: 7,
    name: 'Tabiturient',
    type: 'TABITURIENT',
    baseUrl: 'https://tabiturient.ru/vuzu/dvfu/',
    scheduleEnabled: false,
    scheduleIntervalMinutes: null,
    collectionMode: 'MANUAL',
    description: '',
    isActive: 'true',
  })

  assert.equal(dto.scheduleEnabled, false)
  assert.equal(dto.scheduleIntervalMinutes, null)
  assert.equal(dto.collectionMode, 'MANUAL')
})

test('getUpdateSourceFormValues applies default interval for scheduled source', () => {
  const values = getUpdateSourceFormValues({
    id: 1,
    organization: {
      id: 10,
      name: 'ДВФУ',
      shortName: 'ДВФУ',
    },
    name: 'Tabiturient',
    type: 'TABITURIENT',
    baseUrl: 'https://tabiturient.ru/vuzu/dvfu/',
    collectionMode: 'MANUAL',
    scheduleEnabled: true,
    scheduleIntervalMinutes: null,
    lastCollectedAt: null,
    nextCollectionAt: null,
    description: null,
    createdAt: '2026-04-27T10:00:00Z',
    updatedAt: '2026-04-27T10:00:00Z',
    isActive: true,
  })

  assert.equal(values.scheduleEnabled, true)
  assert.equal(values.scheduleIntervalMinutes, 1440)
})

test('sourceTypeOptions expose Vuzopedia label', () => {
  assert.equal(
    sourceTypeOptions.find((item) => item.value === 'VUZOPEDIA')?.label,
    'Vuzopedia',
  )
})
