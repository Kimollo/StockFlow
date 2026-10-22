export function Banner({ error }) {
  if (!error) {
    return null
  }
  const details = error.errors ? Object.values(error.errors) : []
  return (
    <div className="banner" role="alert">
      <p>{error.message}</p>
      {details.length > 0 && (
        <ul>
          {details.map((message) => (
            <li key={message}>{message}</li>
          ))}
        </ul>
      )}
    </div>
  )
}

export function PageHeader({ eyebrow, title, text, children }) {
  return (
    <header className="page-header">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h2>{title}</h2>
        {text && <p>{text}</p>}
      </div>
      {children}
    </header>
  )
}
