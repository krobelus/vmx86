package org.graalvm.vm.x86.trcview.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.graalvm.vm.memory.util.HexFormatter;
import org.graalvm.vm.x86.node.debug.trace.LocationRecord;
import org.graalvm.vm.x86.node.debug.trace.StepRecord;
import org.graalvm.vm.x86.trcview.io.BlockNode;
import org.graalvm.vm.x86.trcview.io.Node;
import org.graalvm.vm.x86.trcview.io.RecordNode;
import org.graalvm.vm.x86.trcview.ui.event.CallListener;
import org.graalvm.vm.x86.trcview.ui.event.ChangeListener;

import com.everyware.util.log.Trace;

@SuppressWarnings("serial")
public class InstructionView extends JPanel {
    private static final Logger log = Trace.create(InstructionView.class);

    public static final Color CALL_FG = Color.BLUE;
    public static final Color RET_FG = Color.RED;
    public static final Color SYSCALL_FG = Color.MAGENTA;
    public static final Color JMP_FG = Color.LIGHT_GRAY;
    public static final Color JCC_FG = Color.ORANGE;

    private List<Node> instructions;
    private DefaultListModel<String> model;
    private JList<String> insns;

    private List<ChangeListener> changeListeners;
    private List<CallListener> callListeners;

    public InstructionView(Consumer<String> status) {
        super(new BorderLayout());
        changeListeners = new ArrayList<>();
        callListeners = new ArrayList<>();

        insns = new JList<>(model = new DefaultListModel<>());
        insns.setFont(MainWindow.FONT);
        insns.setCellRenderer(new CellRenderer());
        add(BorderLayout.CENTER, new JScrollPane(insns));

        insns.addListSelectionListener(e -> {
            int selected = insns.getSelectedIndex();
            if (selected == -1) {
                return;
            }

            Node node = instructions.get(selected);
            StepRecord step;
            if (node instanceof BlockNode) {
                step = ((BlockNode) node).getHead();
            } else {
                step = (StepRecord) ((RecordNode) node).getRecord();
            }
            LocationRecord loc = step.getLocation();
            StringBuilder buf = new StringBuilder();
            buf.append("PC=0x");
            buf.append(HexFormatter.tohex(loc.getPC(), 16));
            if (loc.getSymbol() != null) {
                buf.append(" ");
                buf.append(loc.getSymbol());
            }
            if (loc.getFilename() != null) {
                buf.append(" [");
                buf.append(loc.getFilename());
                buf.append(" @ 0x");
                buf.append(HexFormatter.tohex(loc.getOffset(), 8));
                buf.append("]");
            }
            status.accept(buf.toString());
            fireChangeEvent();
        });

        insns.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    trace();
                }
            }
        });

        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        insns.getInputMap().put(enter, enter);
        insns.getActionMap().put(enter, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                trace();
            }
        });
    }

    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    protected void fireChangeEvent() {
        for (ChangeListener l : changeListeners) {
            try {
                l.valueChanged();
            } catch (Throwable t) {
                log.log(Level.WARNING, "Error while running listener: " + t, t);
            }
        }
    }

    public void addCallListener(CallListener listener) {
        callListeners.add(listener);
    }

    public void removeCallListener(CallListener listener) {
        callListeners.remove(listener);
    }

    protected void fireCallEvent(BlockNode node) {
        for (CallListener l : callListeners) {
            try {
                l.call(node);
            } catch (Throwable t) {
                log.log(Level.WARNING, "Error while running listener: " + t, t);
            }
        }
    }

    protected void fireRetEvent(RecordNode node) {
        for (CallListener l : callListeners) {
            try {
                l.ret(node);
            } catch (Throwable t) {
                log.log(Level.WARNING, "Error while running listener: " + t, t);
            }
        }
    }

    private void trace() {
        int selected = insns.getSelectedIndex();
        if (selected == -1) {
            return;
        }

        Node node = instructions.get(selected);
        if (node instanceof BlockNode) {
            fireCallEvent((BlockNode) node);
        } else {
            StepRecord step = (StepRecord) ((RecordNode) node).getRecord();
            if (step.getLocation().getMnemonic().equals("ret")) {
                fireRetEvent((RecordNode) node);
            }
        }
    }

    private static String tab(String s, int tabsz) {
        int pos = 0;
        StringBuilder buf = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\t') {
                do {
                    pos++;
                    pos %= tabsz;
                    buf.append(' ');
                } while (pos != 0);
            } else if (c == '\n') {
                pos = 0;
                buf.append(c);
            } else {
                pos++;
                buf.append(c);
            }
        }
        return buf.toString();
    }

    private static String format(StepRecord step) {
        StringBuilder buf = new StringBuilder();
        buf.append("0x");
        buf.append(HexFormatter.tohex(step.getLocation().getPC(), 16));
        buf.append(": ");
        buf.append(tab(step.getLocation().getDisassembly(), 8));
        return buf.toString();
    }

    public void set(BlockNode block) {
        instructions = new ArrayList<>();
        for (Node n : block.getNodes()) {
            if (n instanceof BlockNode) {
                instructions.add(n);
            } else if (n instanceof RecordNode && ((RecordNode) n).getRecord() instanceof StepRecord) {
                instructions.add(n);
            }
        }
        model.removeAllElements();
        for (Node n : instructions) {
            if (n instanceof RecordNode) {
                StepRecord step = (StepRecord) ((RecordNode) n).getRecord();
                model.addElement(format(step));
            } else if (n instanceof BlockNode) {
                StepRecord step = ((BlockNode) n).getHead();
                LocationRecord loc = ((BlockNode) n).getFirstStep().getLocation();
                StringBuilder buf = new StringBuilder(format(step));
                if (loc.getSymbol() != null) {
                    buf.append(" # <");
                    buf.append(loc.getSymbol());
                    buf.append(">");
                    if (loc.getFilename() != null) {
                        buf.append(" [");
                        buf.append(loc.getFilename());
                        buf.append("]");
                    }
                } else if (loc.getFilename() != null) {
                    buf.append(" # 0x");
                    buf.append(HexFormatter.tohex(loc.getPC(), 8));
                    buf.append(" [");
                    buf.append(loc.getFilename());
                    buf.append("]");
                }
                model.addElement(buf.toString());
            }
        }
        insns.setSelectedIndex(0);
        insns.repaint();
    }

    public void select(Node node) {
        for (int n = 0; n < instructions.size(); n++) {
            // ref comparison!
            if (instructions.get(n) == node) {
                insns.setSelectedIndex(n);
                insns.ensureIndexIsVisible(n);
                return;
            }
        }
    }

    public StepRecord getSelectedInstruction() {
        int selected = insns.getSelectedIndex();
        if (selected == -1) {
            return null;
        }

        Node node = instructions.get(selected);
        if (node instanceof BlockNode) {
            return ((BlockNode) node).getHead();
        } else {
            return (StepRecord) ((RecordNode) node).getRecord();
        }
    }

    protected class CellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Node node = instructions.get(index);
            if (node instanceof BlockNode) {
                c.setForeground(CALL_FG);
            } else if (node instanceof RecordNode) {
                StepRecord step = (StepRecord) ((RecordNode) node).getRecord();
                String mnemonic = step.getLocation().getMnemonic();
                switch (mnemonic) {
                    case "ret":
                        c.setForeground(RET_FG);
                        break;
                    case "syscall":
                        c.setForeground(SYSCALL_FG);
                        break;
                    case "jmp":
                        c.setForeground(JMP_FG);
                        break;
                    case "ja":
                    case "jae":
                    case "jb":
                    case "jbe":
                    case "jc":
                    case "jcxz":
                    case "jecxz":
                    case "je":
                    case "jg":
                    case "jge":
                    case "jl":
                    case "jle":
                    case "jna":
                    case "jnae":
                    case "jnb":
                    case "jnbe":
                    case "jnc":
                    case "jne":
                    case "jng":
                    case "jnge":
                    case "jnl":
                    case "jnle":
                    case "jno":
                    case "jnp":
                    case "jns":
                    case "jnz":
                    case "jo":
                    case "jp":
                    case "jpe":
                    case "jpo":
                    case "js":
                    case "jz":
                        c.setForeground(JCC_FG);
                        break;
                }
            }
            return c;
        }
    }
}
