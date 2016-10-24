clear;

muRows = 30*randn(10,2);%[10 10; 0 0; -10 10];
k = 5;
n = 250;
d = size(muRows,2);

ptRows = zeros(0,d);
for i=1:size(muRows,1),
    ptRows = [ptRows; randn(n,d)+repmat(muRows(i,:), [n 1])];
end

clf; hold on;



[idx,C] = kmeans(ptRows,k);

for i=1:k,
    plot(ptRows(idx==i,1),ptRows(idx==i,2),'.');
    plot(C(i,1),C(i,2),'*','markerSize',20);
end