package com.example.posapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.OperationLog;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * محول قائمة أرشيف العمليات
 */
public class OperationLogAdapter extends RecyclerView.Adapter<OperationLogAdapter.OperationLogViewHolder> {
    
    private List<OperationLog> operationLogs;
    private Context context;
    private OnOperationLogClickListener clickListener;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    
    public interface OnOperationLogClickListener {
        void onOperationLogClick(OperationLog operationLog);
    }
    
    public OperationLogAdapter(Context context, List<OperationLog> operationLogs) {
        this.context = context;
        this.operationLogs = operationLogs;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
    }
    
    public void setOnOperationLogClickListener(OnOperationLogClickListener listener) {
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public OperationLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_operation_log, parent, false);
        return new OperationLogViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull OperationLogViewHolder holder, int position) {
        OperationLog operationLog = operationLogs.get(position);
        holder.bind(operationLog);
    }
    
    @Override
    public int getItemCount() {
        return operationLogs != null ? operationLogs.size() : 0;
    }
    
    public void updateOperationLogs(List<OperationLog> newLogs) {
        this.operationLogs = newLogs;
        notifyDataSetChanged();
    }
    
    class OperationLogViewHolder extends RecyclerView.ViewHolder {
        private TextView operationTypeTextView;
        private TextView operationTimeTextView;
        private TextView operationDescriptionTextView;
        private TextView userNameTextView;
        private TextView entityTypeTextView;
        private View criticalIndicator;
        
        public OperationLogViewHolder(@NonNull View itemView) {
            super(itemView);
            operationTypeTextView = itemView.findViewById(R.id.operationTypeTextView);
            operationTimeTextView = itemView.findViewById(R.id.operationTimeTextView);
            operationDescriptionTextView = itemView.findViewById(R.id.operationDescriptionTextView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            entityTypeTextView = itemView.findViewById(R.id.entityTypeTextView);
            criticalIndicator = itemView.findViewById(R.id.criticalIndicator);
        }
        
        public void bind(OperationLog operationLog) {
            // نوع العملية مع أيقونة
            String operationTypeText = getOperationIcon(operationLog.getOperationType()) + " " + 
                                     operationLog.getOperationType().getArabicName();
            operationTypeTextView.setText(operationTypeText);
            
            // الوقت
            if (operationLog.getTimestamp() != null) {
                String timeText = timeFormat.format(operationLog.getTimestamp().toDate()) + 
                                " " + dateFormat.format(operationLog.getTimestamp().toDate());
                operationTimeTextView.setText(timeText);
            }
            
            // الوصف
            operationDescriptionTextView.setText(operationLog.getDescription());
            
            // اسم المستخدم
            userNameTextView.setText("👤 " + (operationLog.getUserName() != null ? 
                                            operationLog.getUserName() : "مستخدم مجهول"));
            
            // نوع الكائن
            String entityText = getEntityIcon(operationLog.getEntityType()) + " " + 
                              operationLog.getEntityType().getArabicName();
            entityTypeTextView.setText(entityText);
            
            // مؤشر العمليات الحساسة
            if (operationLog.isCriticalOperation()) {
                criticalIndicator.setVisibility(View.VISIBLE);
            } else {
                criticalIndicator.setVisibility(View.GONE);
            }
            
            // تلوين خلفية العنصر حسب نوع العملية
            setBackgroundColor(operationLog.getOperationType());
            
            // مستمع النقر
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onOperationLogClick(operationLog);
                }
            });
        }
        
        private String getOperationIcon(OperationLog.OperationType operationType) {
            switch (operationType) {
                case CREATE: return "➕";
                case UPDATE: return "✏️";
                case DELETE: return "🗑️";
                case VIEW: return "👁️";
                case PRINT: return "🖨️";
                case EXPORT: return "📤";
                case LOGIN: return "🔑";
                case LOGOUT: return "🚪";
                case BACKUP: return "💾";
                case RESTORE: return "♻️";
                default: return "📝";
            }
        }
        
        private String getEntityIcon(OperationLog.EntityType entityType) {
            switch (entityType) {
                case INVOICE: return "📄";
                case PRODUCT: return "📦";
                case CUSTOMER: return "👤";
                case USER: return "👥";
                case REPORT: return "📊";
                case SETTING: return "⚙️";
                case DATABASE: return "🗄️";
                default: return "📋";
            }
        }
        
        private void setBackgroundColor(OperationLog.OperationType operationType) {
            int color;
            switch (operationType) {
                case DELETE:
                    color = context.getResources().getColor(android.R.color.holo_red_light);
                    break;
                case UPDATE:
                    color = context.getResources().getColor(android.R.color.holo_orange_light);
                    break;
                case CREATE:
                    color = context.getResources().getColor(android.R.color.holo_green_light);
                    break;
                case VIEW:
                case PRINT:
                    color = context.getResources().getColor(android.R.color.holo_blue_light);
                    break;
                default:
                    color = context.getResources().getColor(android.R.color.white);
                    break;
            }
            
            // تطبيق اللون مع شفافية خفيفة
            itemView.setBackgroundColor((color & 0x00FFFFFF) | 0x20000000);
        }
    }
}