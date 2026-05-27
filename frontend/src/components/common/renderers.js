export function esc(value) {
  return String(value).replace(/[&<>"']/g, (char) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;',
  })[char])
}

export function pct(value) {
  return Number(String(value).replace('%', ''))
}

function tierClass(grade) {
  return String(grade).toLowerCase().replace('+', '-plus')
}

export function renderTier(grade) {
  return `<span class="tier ${tierClass(grade)}" data-tier="${esc(grade)}"><span>${esc(grade)}</span></span>`
}

export function renderTrait(trait) {
  return `<span class="trait-chip"><img src="${trait.iconUrl}" alt="" /><span>${trait.count ?? ''}</span>${esc(trait.name)}</span>`
}

export function renderChampion(unit) {
  const items = unit.items?.length
    ? `<span class="champion-items">${unit.items.slice(0, 3).map((item) => `<img src="${item}" alt="" />`).join('')}</span>`
    : ''
  return `<span class="champion" title="${esc(unit.name)}"><img src="${unit.imageUrl}" alt="${esc(unit.name)}" /><span>${'★'.repeat(unit.stars)}</span>${items}</span>`
}

export function renderTags(tags) {
  return `<div class="tag-row">${tags.map((tag) => `<span class="tag">${esc(tag)}</span>`).join('')}</div>`
}

export function renderItem(url, name) {
  return `<img class="item-icon" src="${url}" alt="${esc(name)}" title="${esc(name)}" />`
}

export function focusInput(selector) {
  requestAnimationFrame(() => {
    const input = document.querySelector(selector)
    if (!input) return
    input.focus()
    input.setSelectionRange(input.value.length, input.value.length)
  })
}
