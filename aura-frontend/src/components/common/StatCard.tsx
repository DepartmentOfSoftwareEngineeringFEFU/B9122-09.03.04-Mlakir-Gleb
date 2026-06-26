import { Card } from '../ui/Card'

interface StatCardProps {
  title: string
  value: number
}

export function StatCard({ title, value }: StatCardProps) {
  return (
    <Card className="p-5">
      <div className="space-y-2">
        <p className="text-sm font-medium text-slate-500">{title}</p>
        <p className="text-3xl font-semibold tracking-tight text-slate-950">{value}</p>
      </div>
    </Card>
  )
}
